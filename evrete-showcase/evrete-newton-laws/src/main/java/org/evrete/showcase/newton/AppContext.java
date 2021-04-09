package org.evrete.showcase.newton;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.showcase.shared.Utils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


@WebListener
public class AppContext implements ServletContextListener {
    private static KnowledgeService knowledgeService;
    static String DEFAULT_SOURCE;
    static String DEFAULT_PRESETS;

    static KnowledgeService knowledgeService() {
        if (knowledgeService == null) {
            throw new IllegalStateException();
        } else {
            return knowledgeService;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (knowledgeService == null) {
            Configuration configuration = new Configuration();
            knowledgeService = new KnowledgeService(configuration);
            ServletContext ctx = sce.getServletContext();
            try {
                DEFAULT_SOURCE = Utils.readResourceAsString(ctx, "/WEB-INF/default_rules.txt");
                DEFAULT_PRESETS = Utils.readResourceAsString(ctx, "/WEB-INF/default_presets.json");
            } catch (Exception e) {
                throw new IllegalStateException("Can not read default rule sources", e);
            }
        } else {
            throw new IllegalStateException();
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (knowledgeService == null) {
            throw new IllegalStateException();
        } else {
            knowledgeService.shutdown();
        }
    }
}