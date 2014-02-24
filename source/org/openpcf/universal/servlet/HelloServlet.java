/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.servlet;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.openpcf.universal.graph.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Give a description of HelloServlet here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.1
 * @since 2014-02-24
 */
public class HelloServlet extends HttpServlet {

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException {

        String resultText = "";

        TitanGraph g = UniversalGraphFactory.create("/tmp/test-titandb");
        for (Vertex v: g.getVertices("name", "genericUnixLikeOperatingSystem")){
            resultText += "<br />" + v.getProperty("desc");
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String docType =
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 " +
                "Transitional//EN\">\n";

        out.println(docType +
            "<HTML>\n" +
            "<HEAD><TITLE>Hello</TITLE></HEAD>\n" +
            "<BODY BGCOLOR=\"#FDF5E6\">\n" +
            "<H1>Hello</H1>\n" +
                resultText +
            "</BODY></HTML>");
    }


}
