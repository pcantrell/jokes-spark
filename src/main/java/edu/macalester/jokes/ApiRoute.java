package edu.macalester.jokes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.macalester.jokes.model.Joke;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import spark.Request;
import spark.Response;
import spark.ResponseTransformerRoute;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by paul on 2014/3/31.
 */
public abstract class ApiRoute extends ResponseTransformerRoute {
    private static SessionFactory sessionFactory = createSessionFactory();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    protected ApiRoute(String path) {
        super(path);
    }

    @Override
    public String render(Object model) {
        return gson.toJson(model);
    }

    @Override
    public final Object handle(Request req, Response res) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            res.type("application/json");

            Object resBody = doHandle(req, res, session);

            tx.commit();
            return resBody;

        } catch(ConstraintViolationException e) {
            // HTTP codes in the 4xx range mean that the client submitted a bad request.
            // 400 is a good one for validation errors.
            res.status(400);

            Map<String,Object> resBody = new HashMap<String, Object>();
            resBody.put("success", false);
            resBody.put("error", e.getLocalizedMessage());

            // Give the client field-by-field user-readable error messages
            Map<String,String> errors = new HashMap<String, String>();
            for(ConstraintViolation<?> violation : e.getConstraintViolations())
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            resBody.put("validationErrors", errors);

            return resBody;

        } catch(Exception e) {
            // HTTP codes in the 5xx range mean that something went wrong on the server,
            // and it's not necessarily the client's fault.
            res.status(500);

            Map<String,Object> resBody = new HashMap<String, Object>();
            resBody.put("success", false);
            resBody.put("error", e.getLocalizedMessage());
            return resBody;
        } finally {
            if(tx.isActive())
                tx.rollback();
        }
    }

    protected abstract Object doHandle(Request req, Response res, Session session);


    /**
     * Standard Hibernate setup. Why do they not provide a single method instead of
     * requiring all this boiler plate code? It is a mystery!
     */
    private static SessionFactory createSessionFactory() {
        Configuration configuration = new Configuration().configure();
        if(System.getenv("DATABASE_URL") != null)
            configuration.setProperty("hibernate.connection.url", System.getenv("DATABASE_URL"));
        return configuration.buildSessionFactory(
            new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build());
    }
}


































