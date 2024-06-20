package mg.itu.prom16;

import java.io.File;
import java.io.IOException;
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

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.prom16.annotations.AnnotationController;
import mg.itu.prom16.annotations.AnnotationGet;
import mg.itu.prom16.annotations.AnnotationPost;
import mg.itu.prom16.annotations.Param;
import mg.itu.prom16.annotations.ParamObject;
import mg.itu.prom16.models.ModelAndView;

public class FrontController extends HttpServlet {
    private final List<String> listeControllers = new ArrayList<>();
    private final Set<String> verifiedClasses = new HashSet<>();
    HashMap<String, Mapping> urlMaping = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        scanControllers(config);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");

            StringBuffer requestURL = request.getRequestURL();
            String[] requestUrlSplitted = requestURL.toString().split("/");
            String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];

            out.println("<h2>Classe et methode associe a l'url :</h2>");
            if (!urlMaping.containsKey(controllerSearched)) {
                out.println("<p>" + "Aucune methode associee a ce chemin." + "</p>");
            } else {
                Mapping mapping = urlMaping.get(controllerSearched);
                Class<?> clazz = Class.forName(mapping.getClassName());
                Method method = null;

                // Find the method that matches the request type (GET or POST)
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(mapping.getMethodeName())) {
                        if (request.getMethod().equalsIgnoreCase("GET")
                                && m.isAnnotationPresent(AnnotationGet.class)) {
                            method = m;
                            break;
                        } else if (request.getMethod().equalsIgnoreCase("POST")
                                && m.isAnnotationPresent(AnnotationPost.class)) {
                            method = m;
                            break;
                        }
                    }
                }

                if (method == null) {
                    out.println("<p>Aucune méthode correspondante trouvée.</p>");
                    return;
                }

                Object[] parameters = getMethodParameters(method, request);
                Object ob = clazz.getDeclaredConstructor().newInstance();
                Object returnValue = method.invoke(ob, parameters);
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
                    out.println("Type de données non reconnu");
                }
            }

            out.println("</body>");
            out.println("</html>");
            out.close();
        }
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
                System.out.println("Le repertoire n'existe pas: " + directory.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File directory, String packageName) throws Exception {
        System.out.println("Scanning directory: " + directory.getAbsolutePath());

        for (File file : directory.listFiles()) {
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
                        for (Method m : methods) {
                            if (m.isAnnotationPresent(AnnotationGet.class)) {
                                Mapping map = new Mapping(className, m.getName());
                                String valeur = m.getAnnotation(AnnotationGet.class).value();
                                if (urlMaping.containsKey(valeur)) {
                                    throw new Exception("double url" + valeur);
                                } else {
                                    urlMaping.put(valeur, map);
                                }
                            } else if (m.isAnnotationPresent(AnnotationPost.class)) {
                                Mapping map = new Mapping(className, m.getName());
                                String valeur = m.getAnnotation(AnnotationPost.class).value();
                                if (urlMaping.containsKey(valeur)) {
                                    throw new Exception("double url" + valeur);
                                } else {
                                    urlMaping.put(valeur, map);
                                }
                            }
                        }
                        System.out.println("Added controller: " + clazz.getName());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Object convertParameter(String value, Class<?> type) {
        if (value == null) {
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

    private Object[] getMethodParameters(Method method, HttpServletRequest request)throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(Param.class)) {
                Param param = parameters[i].getAnnotation(Param.class);
                String paramValue = request.getParameter(param.value());
                parameterValues[i] = convertParameter(paramValue, parameters[i].getType()); // Assuming all parameters are strings for simplicity
            }
            // Vérifie si le paramètre est annoté avec @RequestObject
            else if (parameters[i].isAnnotationPresent(ParamObject.class)) {
                Class<?> parameterType = parameters[i].getType();  // Récupère le type du paramètre (le type de l'objet à créer)
                Object parameterObject = parameterType.getDeclaredConstructor().newInstance();  // Crée une nouvelle instance de cet objet
    
                // Parcourt tous les champs (fields) de l'objet
                for (Field field : parameterType.getDeclaredFields()) {
                    String fieldName = field.getName();  // Récupère le nom du champ
                    String paramName = parameterType.getSimpleName().toLowerCase() + "." + fieldName;  // Forme le nom du paramètre de la requête attendu
                    String paramValue = request.getParameter(paramName);  // Récupère la valeur du paramètre de la requête

                    // Vérifie si la valeur du paramètre n'est pas null (si elle est trouvée dans la requête)
                    if (paramValue != null) {
                        Object convertedValue = convertParameter(paramValue, field.getType());  // Convertit la valeur de la requête en type de champ requis

                        // Construit le nom du setter
                        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                        Method setter = parameterType.getMethod(setterName, field.getType());  // Récupère la méthode setter correspondante
                        setter.invoke(parameterObject, convertedValue);  // Appelle le setter pour définir la valeur convertie dans le champ de l'objet
                    }
                }
                parameterValues[i] = parameterObject;  // Stocke l'objet créé dans le tableau des arguments
            }
            else{

            }
        }

        return parameterValues;
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