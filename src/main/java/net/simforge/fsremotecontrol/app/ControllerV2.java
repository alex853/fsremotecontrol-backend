package net.simforge.fsremotecontrol.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("service/v2")
@CrossOrigin(origins = "*")
public class ControllerV2 {
    private static final Logger log = LoggerFactory.getLogger(ControllerV2.class);

    private static final Map<String, Map<String, SimVarValue>> sessionData = new HashMap<>();

    @GetMapping("hello-world")
    public String getHelloWorld() {
        return "Hello, World!";
    }

    @PostMapping("sim/poll")
    public ResponseEntity<List<String>> postSimData(
            @RequestParam("session") final String session,
            @RequestBody final Map<String, Object> simData) {
        log.info("Session {} - Getting Sim package with {} values", session, simData.size());

        final Map<String, SimVarValue> data = sessionData.computeIfAbsent(session, (s) -> new TreeMap<>());

        simData.forEach((name, value) -> {
            final SimVarValue svValue = data.get(name);
            if (svValue == null) {
                data.remove(name);
                return;
            }
            svValue.updateValue(value);
        });

        final List<String> newSimVars = data.values().stream()
                .filter(e -> e.noValue)
                .map(e -> e.name)
                .collect(Collectors.toList());
        if (!newSimVars.isEmpty()) {
            log.info("Session {} - Notifying sim client about new variables: {}", session, newSimVars);
        }
        return ResponseEntity.ok(newSimVars);
    }

    @PostMapping("ui/poll")
    public ResponseEntity<Map<String, Object>> getSimData(
            @RequestParam("session") final String session,
            @RequestBody final List<String> requestedSimVars) {
        final Map<String, SimVarValue> data = sessionData.get(session);
        if (data == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        final Map<String, Object> result = new TreeMap<>();
        requestedSimVars.forEach(name -> {
            final SimVarValue svValue = data.get(name);
            if (svValue == null) {
                data.put(name, new SimVarValue(name));
                log.info("Session {} - Adding sim var {} to session", session, name);
                return;
            }

            svValue.markRead();
            if (svValue.noValue) {
                return;
            }

            result.put(name, svValue.value);
        });

        log.info("Session {} - Sending UI package with {} values", session, result.size());
        return ResponseEntity.ok(result);
    }

    private static class SimVarValue {
        private final String name;
        private long lastRead = System.currentTimeMillis();
        private boolean noValue = true;
        private Object value;

        public SimVarValue(final String name) {
            this.name = name;
        }

        public void markRead() {
            lastRead = System.currentTimeMillis();
        }

        public void updateValue(final Object value) {
            this.noValue = false;
            this.value = value;
        }
    }
}
