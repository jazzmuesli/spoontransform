package org.pavelreich.saaremaa;

import graphics.DotGraph;
import groum.GROUMBuilder;
import groum.GROUMGraph;
import groum.GROUMNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by preich on 19/02/19.
 */
public class GroumRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectCreationOccurence.class);
    static void runGroom(String path) {
        GROUMBuilder gb = new GROUMBuilder(path);
        gb.build();
        GROUMGraph groum;
        DotGraph.EXEC_DOT = System.getProperty("dot.path", "dot");
        Iterator iterator = gb.getGroums().iterator();
        while (iterator.hasNext()) {
            groum = (GROUMGraph) iterator.next();
            HashSet<GROUMNode> nodes = groum.getNodes();
            groum.toGraphics("/tmp/");
            List<Object> xs = nodes.stream()
                    .flatMap(x -> x.getOutEdges().stream().map(e -> toString(x) + " => " + toString(e.getDest())))
                    .collect(Collectors.toList());
            xs.forEach(s -> LOG.info("edge: " + String.valueOf(s)));
            LOG.info("nodes: " + nodes);
        }
    }

    private static String toString(GROUMNode node) {
        return node.getLabel();
    }

}
