package org.litesoft.wip;

import org.litesoft.filtering.AbstractFilterWithFilterers;
import org.litesoft.versioning.Version;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LeadFilter extends AbstractFilterWithFilterers {
    public LeadFilter() {
        super( Version.get(), request -> {
            System.out.println( "**** LeadFilter - " + request.getRequestURI() );
            return null;
        } );
    }
}
