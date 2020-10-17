package org.evrete.showcase.stock;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.showcase.shared.Utils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContext implements ServletContextListener {
    static KnowledgeService knowledgeService;
    static String DEFAULT_SOURCE;
    static OHLC[] DEFAULT_STOCK_HISTORY;

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
            configuration.setProperty("org.evrete.minimal.condition-base-class", ConditionSuperClass.class.getName());
            knowledgeService = new KnowledgeService(configuration);
            ServletContext ctx = sce.getServletContext();
            try {
                DEFAULT_SOURCE = Utils.readResourceAsString(ctx, "/WEB-INF/default_rules.txt");
                DEFAULT_STOCK_HISTORY = Utils.fromJson(
                        Utils.readResourceAsString(ctx, "/WEB-INF/default_stock_data.json"),
                        OHLC[].class
                );
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