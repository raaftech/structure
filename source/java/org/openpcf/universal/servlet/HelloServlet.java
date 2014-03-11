/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.servlet;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.openpcf.universal.initialization.UniversalGraphSingleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Give a description of HelloServlet here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-24
 */
public class HelloServlet extends HttpServlet {

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException {

        String resultText = "";

        TitanGraph g = UniversalGraphSingleton.INSTANCE.getGraph();
        for (Vertex v : g.getVertices("name", "genericUnixLikeOperatingSystem")) {
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
