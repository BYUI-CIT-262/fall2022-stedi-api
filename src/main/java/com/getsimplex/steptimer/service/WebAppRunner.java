package com.getsimplex.steptimer.service;

/**
 © 2021 Sean Murdock
 * Created by sean on 8/10/2016 based on https://github.com/tipsy/spark-websocket/tree/master/src/main/java
 */


import com.getsimplex.steptimer.model.*;
import com.getsimplex.steptimer.utils.*;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static spark.Spark.*;

public class WebAppRunner {

    public static void main(String[] args){

        Spark.port(getHerokuAssignedPort());

		//secure("/Applications/steptimerwebsocket/keystore.jks","password","/Applications/steptimerwebsocket/keystore.jks","password");
        staticFileLocation("/public");
        webSocket("/socket", DeviceWebSocketHandler.class);
        webSocket("/timeruiwebsocket", TimerUIWebSocket.class);
        //post("/sensorUpdates", (req, res)-> WebServiceHandler.routeDeviceRequest(req));
        //post("/generateHistoricalGraph", (req, res)->routePdfRequest(req, res));
        //get("/readPdf", (req, res)->routePdfRequest(req, res));
        post("/user", (req, res)-> {
            String response="Error creating user";
            try {
                response = callUserDatabase(req);
            } catch (AlreadyExistsException ae){
                res.status(409);
                System.out.println("User already exists");
            } catch (Exception e){
                res.status(500);
                System.out.println("Error creating user");
            }
            return response;
        }
            );
        get("/validate/:token", (req,res)->SessionValidator.emailFromToken(req.params(":token")));
        get("/simulation", (req, res) -> SimulationDataDriver.getSimulationActive());
        post("/simulation", (req, res)-> MessageIntake.route(new StartSimulation(30)));
        delete("/simulation", (req, res)-> MessageIntake.route(new StopSimulation()));

        get ("/stephistory/:customer", (req, res)-> {
            try{
                userFilter(req, res);
            } catch (Exception e){
                res.redirect("/");
            }
            return StepHistory.getAllTests(req.params(":customer"));
        });
        post("/customer", (req, res)-> {
            String response;
            try {
                createNewCustomer(req, res);
                response="Successfully created customer";
            }

            catch (AlreadyExistsException ae){
                System.out.println("User already exists");
                res.status(409);
                response="Error creating customer";
            }

            catch (Exception e){
                System.out.println("*** Error Creating Customer: "+e.getMessage());
                res.status(500);
                response="Error creating customer";
            }
            return response;
        });
        get("/customer/:customer", (req, res)-> {
            try {
                userFilter(req, res);
            } catch (Exception e){
                res.status(401);
                System.out.println("*** Error Finding Customer: "+e.getMessage());
                return null;
            }
            return FindCustomer.handleRequest(req);

        });

        post("/login", (req, res)->loginUser(req));
        post("/twofactorlogin/:phoneNumber",(req, res) -> twoFactorLogin(req, res));
        post("/twofactorlogin", (req, res) ->{
            String response = "";
           try{
               response=OneTimePasswordService.handleRequest(req);
           } catch (NotFoundException nfe){
               res.status(404);
               response= nfe.getMessage();
           } catch (ExpiredException ee){
               res.status(401);
               response= ee.getMessage();
           } catch (Exception e){
                res.status(500);
                response = "Unexpected error";
           }
            return response;
        });
        post("/rapidsteptest", (req, res)->{
            try{
                userFilter(req, res);
            } catch (Exception e){
                res.status(401);
            }

            saveStepSession(req);
            return "Saved";
        });
        get("/riskscore/:customer",((req,res) -> {
            try{
                userFilter(req, res);
            } catch (Exception e){
                res.status(401);
                System.out.println("*** Error Finding Risk Score: "+e.getMessage());
                throw e;
            }
            return riskScore(req.params(":customer"));
        }));


        init();
    }
    private static String twoFactorLogin(Request request, Response response){
        String phoneNumber =  request.params(":phoneNumber");
        int randomNum = ThreadLocalRandom.current().nextInt(1111, 10000);
        User user=null;
        try {
            phoneNumber = SendText.getFormattedPhone(phoneNumber);
            user = UserService.getUserByPhone(phoneNumber);
            if (user!=null){
                Long expiration = new Date().getTime()+100l * 365l * 24l *60l * 60l *1000l;//100 years
                String loginToken=TokenService.createUserTokenSpecificTimeout(user.getUserName(), expiration);
                OneTimePassword oneTimePassword = new OneTimePassword();
                oneTimePassword.setOneTimePassword(randomNum);
                oneTimePassword.setExpirationDate(new Date(System.currentTimeMillis()+60l*30l*1000l));
                oneTimePassword.setLoginToken(loginToken);
                oneTimePassword.setPhoneNumber(phoneNumber);
                OneTimePasswordService.saveOneTimePassword(oneTimePassword);

                SendText.send(phoneNumber, "STEDI OTP: "+String.valueOf(randomNum));
                response.status(200);

            } else{
                response.status(400);
                System.out.println("Unable to find user with phone number: "+phoneNumber);

            }
        } catch (Exception e){
            response.status(500);
            System.out.println("Error while looking up user "+phoneNumber+" "+e.getMessage());
        }

        if (user==null){
            return "Unable to find user with phone number: "+phoneNumber;
        } else{
            return "Ok";
        }
    }

    private static void userFilter(Request request, Response response) throws Exception{
        String tokenString = request.headers("suresteps.session.token");

            Optional<User> user = TokenService.getUserFromToken(tokenString);//

            Boolean tokenExpired = SessionValidator.validateToken(tokenString);

            if (user.isPresent() && tokenExpired && !user.get().isLocked()){//if a user is locked, we won't renew tokens until they are unlocked
                TokenService.renewToken(tokenString);
                return;
            }

            if (!user.isPresent()) { //Check to see if session expired
                throw new Exception("Invalid user token: user not found using token: "+tokenString);
            }

            if (tokenExpired.equals(true)){
                throw new Exception("Invalid user token: "+tokenString+" expired");
            }

    }



    public static void createNewCustomer(Request request, Response response) throws Exception{
            CreateNewCustomer.handleRequest(request);
    }

    private static String callUserDatabase(Request request)throws Exception{
        return CreateNewUser.handleRequest(request);
    }

    private static String loginUser(Request request) throws Exception{
        return LoginController.handleRequest(request);

    }

    private static String riskScore(String email) throws Exception{
        return StepHistory.riskScore(email);
    }

    private static void saveStepSession(Request request) throws Exception{
        SaveRapidStepTest.save(request.body());
    }

	
    private static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return Configuration.getConfiguration().getInt("suresteps.port"); //return default port if heroku-port isn't set (i.e. on localhost)
    }

}
