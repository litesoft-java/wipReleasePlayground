package org.litesoft.wip;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("DefaultAnnotationParam")
@Order(Ordered.LOWEST_PRECEDENCE)
public class Filter2 extends AbstractFilter {
    public Filter2() {
        super( "Filter2" );
    }
}
