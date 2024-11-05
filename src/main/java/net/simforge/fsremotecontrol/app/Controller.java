package net.simforge.fsremotecontrol.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("service/v1")
@CrossOrigin
public class Controller {
    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private static final Map<String, Map<String, Object>> sessionData = new HashMap<>();

    @GetMapping("hello-world")
    public String getHelloWorld() {
        return "Hello, World!";
    }

    @PostMapping("sim/post")
    public ResponseEntity<Void> postSimData(@RequestBody final Map<String, Object> data) {
        final String session = (String) data.get("session");
        sessionData.put(session, data);
        return ResponseEntity.ok().build();
    }

    @GetMapping("ui/get")
    public ResponseEntity<Map<String, Object>> getSimData(@RequestParam("session") final String session) {
        final Map<String, Object> data = sessionData.get(session);
        return ResponseEntity.ok(data);
    }
}
