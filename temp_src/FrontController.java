package mg.itu.prom16;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import mg.itu.prom16.annotations.AnnotationController;
import mg.itu.prom16.annotations.OnErrorValidation;
import mg.itu.prom16.annotations.AnnotationGet;
import mg.itu.prom16.annotations.AnnotationPost;
import mg.itu.prom16.annotations.Auth;
import mg.itu.prom16.annotations.Param;
import mg.itu.prom16.annotations.ParamObject;
import mg.itu.prom16.annotations.RequestParam;
import mg.itu.prom16.annotations.Required;
import mg.itu.prom16.annotations.RestAPI;
import mg.itu.prom16.annotations.TypeDouble;
import mg.itu.prom16.annotations.TypeInt;
import mg.itu.prom16.annotations.Range;
import mg.itu.prom16.annotations.Url;
import mg.itu.prom16.models.ModelAndView;
import mg.itu.prom16.util.ConfigManager;
import mg.itu.prom16.util.Mapping;
import mg.itu.prom16.util.ValidationValue;
import mg.itu.prom16.util.VerbAction;
import mg.itu.prom16.util.ValidationsError;

@MultipartConfig
public class FrontController extends HttpServlet {
    private final List<String> listeControllers = new ArrayList<>();
    private final Set<String> verifiedClasses = new HashSet<>();
    HashMap<String, Mapping> urlMaping = new HashMap<>();
    String error = "";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        scanControllers(config);
        ConfigManager.init(config);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        int errorCode = 0; // Code d'erreur par défaut (aucune erreur)
        String errorMessage = "Une erreur inattendue est survenue.";
        String errorDetails = null;
        ValidationsError validationsErrors = new ValidationsError();
        ValidationValue validationValue = new ValidationValue();
        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");

            StringBuffer requestURL = request.getRequestURL();
            String[] requestUrlSplitted = requestURL.toString().split("/");
            String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];

            out.println("<h2>Classe et methode associe a l'url :</h2>");
            if (!error.isEmpty()) {
                errorCode = 400;
                errorMessage = "Erreur de demande";
                errorDetails = error;
                displayErrorPage(out, errorCode, errorMessage, errorDetails, controllerSearched, request.getMethod());

                return;
            } else if (!urlMaping.containsKey(controllerSearched)) {
                errorCode = 404;
                errorMessage = "Non trouvé";
                errorDetails = "Aucune méthode associée au chemin spécifié.";
                displayErrorPage(out, errorCode, errorMessage, errorDetails, controllerSearched, request.getMethod());

                return;
            } else {
                Mapping mapping = urlMaping.get(controllerSearched);
                Class<?> clazz = Class.forName(mapping.getClassName());
                Method method = null;

                if (!mapping.isVerbAction(request.getMethod())) {
                    errorCode = 405;
                    errorMessage = "Méthode non autorisée";
                    errorDetails = "Le verbe HTTP utilisé n'est pas pris en charge pour cette action.";
                    displayErrorPage(out, errorCode, errorMessage, errorDetails, controllerSearched,
                            request.getMethod());

                    return;
                }

                for (Method m : clazz.getDeclaredMethods()) {
                    for (VerbAction action : mapping.getVerbActions()) {
                        if (m.getName().equals(action.getMethodeName())
                                && action.getVerb().equalsIgnoreCase(request.getMethod())) {
                            method = m;
                            break;
                        }
                    }
                    if (method != null) {
                        break;
                    }
                }

                if (method == null) {
                    errorCode = 404;
                    errorMessage = "Non trouvé";
                    errorDetails = "Aucune méthode correspondante trouvée.";
                    displayErrorPage(out, errorCode, errorMessage, errorDetails, controllerSearched,
                            request.getMethod());
                    return;
                }
                CustomSession session = new CustomSession(request.getSession());
                StringBuilder authError = new StringBuilder();
                if (!session.checkAuthorization(clazz, method, authError)) {
                    request.setAttribute("authErro", authError.toString()); // Mettre à jour la requête avec le message
                                                                            // d'erreur
                    request.getRequestDispatcher(ConfigManager.getLoginUrl()).forward(request, response);
                }

                Object[] parameters = getMethodParameters(method, request, validationsErrors, validationValue);
                if (validationsErrors.hasErrors()) {
                    String url = findErrorRedirectUrl(clazz, method);
                    VerbAction verbError = findErrorRedirectVerb(urlMaping, url);
                    HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                        @Override
                        public String getMethod() {
                            return "GET"; // Forcer la méthode à "GET"
                        }
                    };
                    if (verbError.getVerb().equalsIgnoreCase("GET")) {
                        System.out.println("method get");

                        wrappedRequest.setAttribute("validationErrors",
                                validationsErrors.getAllErrors());
                        wrappedRequest.setAttribute("validationValue",
                                validationValue.getAllValue());
                        wrappedRequest.getRequestDispatcher(url).forward(wrappedRequest,
                                response);
                    } else {
                        System.out.println("method post");
                        request.setAttribute("validationValue",
                                validationValue.getAllValue());
                        request.setAttribute("validationErrors", validationsErrors.getAllErrors());
                        request.getRequestDispatcher(url).forward(request, response);
                    }
                }
                Object ob = clazz.getDeclaredConstructor().newInstance();
                verifieCustomSession(ob, request);
                Object returnValue = method.invoke(ob, parameters);
                if (method.isAnnotationPresent(RestAPI.class)) {
                    response.setContentType("application/json");
                    Gson gson = new Gson();
                    String stringResponse;
                    if (returnValue instanceof String) {
                        stringResponse = gson.toJson(returnValue);
                        out.print(stringResponse);
                    } else if (returnValue instanceof ModelAndView) {
                        ModelAndView modelAndView = (ModelAndView) returnValue;
                        stringResponse = gson.toJson(modelAndView.getData());
                        out.print(stringResponse);
                    } else {
                        errorCode = 500;
                        errorMessage = "Erreur interne du serveur";
                        errorDetails = "Type de données non reconnu.";
                        displayErrorPage(out, errorCode, errorMessage, errorDetails, controllerSearched,
                                request.getMethod());
                        return;
                    }
                } else {
                    if (returnValue instanceof String) {
                        out.println("La valeur de retour est " + (String) returnValue);
                    } else if (returnValue instanceof ModelAndView) {
                        ModelAndView modelAndView = (ModelAndView) returnValue;
                        for (Map.Entry<String, Object> entry : modelAndView.getData().entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                        RequestDispatcher dispatcher = request.getRequestDispatcher(modelAndView.getUrl());
                        dispatcher.forward(request, response);
                    } else {
                        errorCode = 500;
                        errorMessage = "Erreur interne du serveur";
                        errorDetails = "Type de données non reconnu.";
                        displayErrorPage(out, errorCode, errorMessage, errorDetails, controllerSearched,
                                request.getMethod());
                        return;
                    }
                }
            }

            out.println("</body>");
            out.println("</html>");
            out.close();
        } catch (Exception e) {

            errorCode = 500;
            errorMessage = "Erreur interne du serveur";
            errorDetails = e.getMessage();
            displayErrorPage(out, errorCode, errorMessage, errorDetails, "error", request.getMethod());

        }
    }

    private static String findErrorRedirectUrl(Class<?> controllerClass, Method method) {
        System.out.println("findErrorRedirectUrl");
        // Vérifiez si l'annotation @OnErrorValidation est présente sur la méthode
        if (method.isAnnotationPresent(OnErrorValidation.class)) {
            System.out.println("method: " + method.getAnnotation(OnErrorValidation.class).value());
            return method.getAnnotation(OnErrorValidation.class).value();
        }
        // Vérifiez si l'annotation @OnErrorValidation est présente sur la classe
        if (controllerClass.isAnnotationPresent(OnErrorValidation.class)) {
            System.out.println("controllerclass: " + controllerClass.getAnnotation(OnErrorValidation.class).value());
            return controllerClass.getAnnotation(OnErrorValidation.class).value();
        }
        // Sinon, retournez une valeur par défaut ou null
        System.out.println("no url found");
        return null;
    }

    private static VerbAction findErrorRedirectVerb(HashMap<String, Mapping> hashMap, String errorRedirectUrl) {
        System.out.println("findErrorRedirectVerb");
        Mapping mapping = hashMap.get(errorRedirectUrl);
        if (mapping != null && !mapping.getVerbActions().isEmpty()) {
            System.out.println("verbACtion found: " + mapping.getVerbActions().get(0).getVerb());
            // Retourner le premier verbe associé à cette URL
            return mapping.getVerbActions().get(0);
        }
        System.out.println("no verbAction found");
        return null;
    }

    private void scanControllers(ServletConfig config) {
        String controllerPackage = config.getInitParameter("controller-package");
        System.out.println("Scanning package: " + controllerPackage);

        // Scanner les classes du package donné dans WEB-INF/classes
        try {
            String path = "WEB-INF/classes/" + controllerPackage.replace('.', '/');
            File directory = new File(getServletContext().getRealPath(path));
            if (directory.exists()) {
                scanDirectory(directory, controllerPackage);
            } else {
                throw new Exception("Le repertoire n'existe pas: " + directory.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File directory, String packageName) throws Exception {
        System.out.println("Scanning directory: " + directory.getAbsolutePath());
        File[] files = directory.listFiles();

        System.out.println("Nombre de fichiers trouvés dans " + directory.getAbsolutePath() + " : " + files.length);

        for (File file : files) {
            try {
                System.out.println("Processing file: " + file.getName());

                if (file.isDirectory()) {
                    scanDirectory(file, packageName + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(AnnotationController.class)
                                && !verifiedClasses.contains(clazz.getName())) {
                            AnnotationController annotation = clazz.getAnnotation(AnnotationController.class);
                            listeControllers.add(clazz.getName() + " (" + annotation.value() + ")");
                            verifiedClasses.add(clazz.getName());
                            Method[] methods = clazz.getMethods();
                            for (Method method : methods) {
                                if (method.getDeclaringClass().equals(Object.class)) {
                                    continue; // Ignore les méthodes héritées comme equals(), hashCode(), etc.
                                }
                                if (method.isAnnotationPresent(Url.class)) {
                                    Url urlAnnotation = method.getAnnotation(Url.class);
                                    String url = urlAnnotation.value();
                                    String verb = "GET";
                                    if (method.isAnnotationPresent(AnnotationGet.class)) {
                                        verb = "GET";
                                    } else if (method.isAnnotationPresent(AnnotationPost.class)) {
                                        verb = "POST";
                                    }
                                    VerbAction verbAction = new VerbAction(method.getName(), verb);
                                    Mapping map = new Mapping(className);
                                    if (urlMaping.containsKey(url)) {
                                        Mapping existingMap = urlMaping.get(url);
                                        if (existingMap.isVerbPresent(verbAction)) {
                                            throw new Exception("Duplicate URL: " + url);
                                        } else {
                                            existingMap.setVerbActions(verbAction);
                                        }
                                    } else {
                                        map.setVerbActions(verbAction);
                                        System.out.println("ClassName: " + className + " url " + url + "");
                                        urlMaping.put(url, map);
                                    }
                                } else {
                                    System.out.println("ClassName: " + className + " method " + method.getName()
                                            + " doit etre annoté en url");
                                    throw new Exception(
                                            "il faut avoir une annotation url dans le controlleur  " + className);
                                }
                            }
                            System.out.println("Added controller: " + clazz.getName());
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        System.out.println(e);
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
    }

    public static Object convertParameter(String value, Class<?> type) {
        System.out.println("convertParameter");
        if (value == null || value.equals("")) {
            System.out.println("convertParameter null");
            return null;
        }
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        // Ajoutez d'autres conversions nécessaires ici
        return null;
    }

    private Object[] getMethodParameters(Method method, HttpServletRequest request, ValidationsError validationsError,
            ValidationValue validationValue)
            throws Exception {
        System.out.println("getMethodParameters");
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isAnnotationPresent(Param.class)
                    && !parameters[i].isAnnotationPresent(ParamObject.class)
                    && !parameters[i].getType().equals(CustomSession.class)) {
                throw new Exception("ETU002380: les attributs doivent etre annoter par Param ou ParamObject");
            }
            if (parameters[i].getType().equals(CustomSession.class)) {
                CustomSession session = new CustomSession(request.getSession());
                parameterValues[i] = session;
            }
            if (parameters[i].isAnnotationPresent(Param.class)) {
                Param param = parameters[i].getAnnotation(Param.class);
                if (parameters[i].getType() == Part.class) {
                    Part file = request.getPart(param.value());
                    upload(file);
                    parameterValues[i] = file;
                } else {
                    String paramValue = request.getParameter(param.value());
                    parameterValues[i] = convertParameter(paramValue, parameters[i].getType());
                }
            }
            // Vérifie si le paramètre est annoté avec @RequestObject
            else if (parameters[i].isAnnotationPresent(ParamObject.class)) {
                Class<?> parameterType = parameters[i].getType();
                Object parameterObject = parameterType.getDeclaredConstructor().newInstance();
                for (Field field : parameterType.getDeclaredFields()) {
                    RequestParam param = field.getAnnotation(RequestParam.class);
                    String fieldName = field.getName(); // Récupère le nom du champ
                    // parameterType.getSimpleName().toLowerCase() + "." +
                    String paramName = (param != null) ? param.value() : fieldName;
                    String paramValue = request.getParameter(paramName);
                    if (paramValue != null) {
                        validateFieldValue(paramValue, field, validationsError, validationValue);
                        // if (validationsError.getAllErrors().get(fieldName) == null ||
                        // paramValue.equals("")) {
                        Object convertedValue = convertParameter(paramValue, field.getType());
                        System.out.println("valeur du paramValue" + paramName + ":" + convertedValue + "");
                        // Construit le nom du setter
                        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0))
                                + fieldName.substring(1);
                        Method setter = parameterType.getMethod(setterName, field.getType()); // Récupère la méthode
                                                                                              // setter
                                                                                              // correspondante
                        if (convertedValue != null || !field.getType().isPrimitive()) {
                            setter.invoke(parameterObject, convertedValue);
                        }

                        // setter.invoke(parameterObject, convertedValue); // Appelle le setter pour
                        // définir la valeur
                        // convertie dans le champ de l'objet
                        // }
                    }
                }
                parameterValues[i] = parameterObject; // Stocke l'objet créé dans le tableau des arguments
            } else {

            }
        }
        System.out.println("nivoaka getmethodparameter");
        return parameterValues;
    }

    public void upload(Part filePart) throws Exception {
        // Obtenir le nom de fichier
        String fileName = filePart.getSubmittedFileName();

        // Chemin où vous souhaitez enregistrer le fichier
        String uploadPath = "D:/ITU/S5/upload/" + fileName;

        // Lire le fichier et le stocker
        try (InputStream fileContent = filePart.getInputStream();
                FileOutputStream fos = new FileOutputStream(new File(uploadPath))) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileContent.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new Exception("Erreur lors du téléchargement : " + e.getMessage());
        }
    }

    public void verifieCustomSession(Object o, HttpServletRequest request) throws Exception {
        Class<?> c = o.getClass();
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(CustomSession.class)) {
                Method sessionMethod = c.getMethod("setSession", CustomSession.class);
                CustomSession session = new CustomSession(request.getSession());
                sessionMethod.invoke(o, session);
                return;
            }
        }
    }

    private void displayErrorPage(PrintWriter out, int errorCode, String errorMessage, String errorDetails,
            String controllerSearched, String method) {
        out.println("<html>");
        out.println("<head><title>Erreur " + errorCode + "</title></head>");
        out.println("<body>");
        out.println("<div style='font-family: Arial, sans-serif; max-width: 600px;margin: auto;'>");
        out.println("<h1 style='color: #e74c3c;'>" + errorMessage + "</h1>");
        out.println("<p><strong>Code d'erreur :</strong> " + errorCode + "</p>");
        out.println("<p>" + errorDetails + "</p>");
        out.println("<p><strong>Action demandée :</strong> " + controllerSearched + "</p>");
        out.println("<p><strong>Méthode HTTP utilisée :</strong> " + method + "</p>");
        out.println("<a href='/' style='color: #3498db;'>Retour à l'accueil</a>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }

    public void validateFieldValue(String paramValue, Field field, ValidationsError validationsError,
            ValidationValue validationValue) throws Exception {
        System.out.println("validateFieldValue");
        // Vérifie @Required
        if (field.isAnnotationPresent(Required.class)) {
            Required required = field.getAnnotation(Required.class);
            if (paramValue.isEmpty()) {
                validationsError.addError(field.getName(), required.message());
                System.out.println(required.message());
            }
        }

        // Vérifie @Decimal
        if (field.isAnnotationPresent(TypeDouble.class)) {
            TypeDouble TypeDouble = field.getAnnotation(TypeDouble.class);
            try {
                Double.parseDouble(paramValue); // Vérifie si paramValue est un décimal
            } catch (NumberFormatException e) {
                validationsError.addError(field.getName(), TypeDouble.message());
                System.out.println(TypeDouble.message());
            }
        }

        // Vérifie @TypeInt
        if (field.isAnnotationPresent(TypeInt.class)) {
            TypeInt typeInt = field.getAnnotation(TypeInt.class);
            try {
                Integer.parseInt(paramValue); // Vérifie si paramValue est un entier
            } catch (NumberFormatException e) {
                validationsError.addError(field.getName(), typeInt.message());
                System.out.println(typeInt.message());
            }
        }

        // Vérifie @Range
        if (field.isAnnotationPresent(Range.class)) {
            Range range = field.getAnnotation(Range.class);
            try {
                double doubleValue = Double.parseDouble(paramValue);
                if (doubleValue < range.min() || doubleValue > range.max()) {
                    validationsError.addError(field.getName(), range.message());
                    // throw new Exception(range.message());
                }
            } catch (NumberFormatException e) {
                validationsError.addError(field.getName(), range.message());
            }
        }
        validationValue.addValue(field.getName(), paramValue);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {

            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}