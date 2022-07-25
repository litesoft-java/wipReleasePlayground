package org.litesoft.wip;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class RequestHandler {

    @GetMapping("/a")
    public ResponseEntity<String> a() {
        return generateResponse( "a" );
    }

    @GetMapping("/b")
    public ResponseEntity<String> b() {
        return generateResponse( "b" );
    }

    private ResponseEntity<String> generateResponse( String which ) {
        String msg = which + " Called";
        System.out.println("---- " + msg);
        return ResponseEntity.ok( msg );
    }
}
