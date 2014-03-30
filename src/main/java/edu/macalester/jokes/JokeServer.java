package edu.macalester.jokes;

import com.google.gson.Gson;
import edu.macalester.jokes.model.Joke;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.sql.Time;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class JokeServer {

    public static void main(String[] args) {
        final SessionFactory sessionFactory = createSessionFactory();

        // Heroku passes the port to listen on in the PORT environment variable.
        // The app won't start on Heroku unless we listen on this port.
        if(System.getenv("PORT") != null)
            setPort(Integer.parseInt(System.getenv("PORT")));

        // This is an API-only server; all responses are JSON.
        before(new Filter() {
            @Override
            public void handle(Request req, Response res) {
                res.type("application/json");
            }
        });

        get(new Route("/jokes") {
            @Override
            public Object handle(Request req, Response res) {
                Session session = sessionFactory.openSession();
                return new Gson().toJson(
                    session.createQuery("from Joke").list());
            }
        });

        get(new Route("/jokes/:id") {
            @Override
            public Object handle(Request req, Response res) {
                Session session = sessionFactory.openSession();
                return new Gson().toJson(
                    session.get(Joke.class, Long.parseLong(req.params("id"))));
            }
        });

        post(new Route("/jokes") {
            @Override
            public Object handle(Request req, Response res) {
                Session session = sessionFactory.openSession();
                Transaction tx = session.beginTransaction();
                try {
                    Joke joke = new Joke();
                    joke.setSetup(req.queryParams("setup"));
                    joke.setPunchline(req.queryParams("punchline"));
                    session.save(joke);
                    tx.commit();

                    Map<String,Object> resBody = new HashMap<String, Object>();
                    resBody.put("success", true);
                    resBody.put("joke", joke);
                    return new Gson().toJson(resBody);

                } catch(ConstraintViolationException e) {
                    tx.rollback();

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

                    return new Gson().toJson(resBody);

                } catch(Exception e) {
                    tx.rollback();

                    // HTTP codes in the 5xx range mean that something went wrong on the server,
                    // and it's not necessarily the client's fault.
                    res.status(500);

                    Map<String,Object> resBody = new HashMap<String, Object>();
                    resBody.put("success", false);
                    resBody.put("error", e.getLocalizedMessage());
                    return new Gson().toJson(resBody);
                }
            }
        });
    }

    /**
     * Standard Hibernate setup. Why do they not provide a single method instead of
     * requiring all this boiler plate code? It is a mystery!
     */
    private static SessionFactory createSessionFactory() {
        Configuration configuration = new Configuration().configure();
        return configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties())
                        .build());
    }
}
