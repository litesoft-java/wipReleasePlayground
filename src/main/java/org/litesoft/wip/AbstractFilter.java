package org.litesoft.wip;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractFilter implements Filter {
    private final String filterName;
    private final String appVersion;

    public AbstractFilter( String filterName ) {
        this.filterName = filterName;
        appVersion = Version.get();
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain )
            throws IOException, ServletException {

        if ( shouldForward( (HttpServletRequest)request, (HttpServletResponse)response ) ) {
            chain.doFilter( request, response );
        }
    }

    protected boolean shouldForward( HttpServletRequest request, HttpServletResponse response )
            throws IOException {
        if ( "/AppVersion".equals( request.getServletPath() ) ) {
            return sendSimpleAnswer( response, appVersion );
        }
        System.out.println( "**** " + filterName + " - " + request.getRequestURI() );
        return true;
    }

    protected boolean sendSimpleAnswer( HttpServletResponse response, String answer )
            throws IOException {
        response.setStatus( 200 );
        PrintWriter writer = response.getWriter();
        writer.print( answer );
        writer.close();
        return false; // convenience -> don't forward!
    }
}
