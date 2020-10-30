package org.evrete.showcase.chess;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


@WebListener
public class AppContext implements ServletContextListener {
    static KnowledgeService knowledgeService;

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