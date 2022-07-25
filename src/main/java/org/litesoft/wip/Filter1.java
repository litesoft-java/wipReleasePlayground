package org.litesoft.wip;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class Filter1 extends AbstractTestFilter {
    public Filter1() {
        super( "Filter1" );
    }
}
