package org.litesoft.wip;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractTestFilter implements Filter {
    private final String filterName ;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        System.out.println("**** " + filterName + " - " + ((HttpServletRequest) request).getRequestURI());
        chain.doFilter(request, response);
    }
}
