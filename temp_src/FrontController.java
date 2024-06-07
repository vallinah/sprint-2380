package mg.itu.prom16;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.prom16.models.ModelAndView;

public class FrontController extends HttpServlet {
    private final List<String> listeControllers = new ArrayList<>();
    private final Set<String> verifiedClasses = new HashSet<>();
    private String controllerPackage;
    HashMap<String, Mapping> urlMaping = new HashMap<>();
    String error = "";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllerPackage = getInitParameter("controller-package");
        try {
            this.scanControllers(config);
        } catch (Exception e) {
            error = e.getMessage();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, NoSuchMethodException, SecurityException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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
            if (error != "") {
                out.println(error);
            } else if (!urlMaping.containsKey(controllerSearched)) {
                out.println("<p>" + "Aucune methode associee a ce chemin." + "</p>");
            } else {
                Mapping mapping = urlMaping.get(controllerSearched);
                Class<?> clazz = Class.forName(mapping.getClassName());
                Method method = clazz.getMethod(mapping.getMethodeName());
                Object ob = clazz.getDeclaredConstructor().newInstance();
                Object returnValue = method.invoke(ob);
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

    private void scanControllers(ServletConfig config) throws Exception {
        System.out.println("Scanning package: " + controllerPackage);

        // Scanner les classes du package donné dans WEB-INF/classes
        try {
            String path = "WEB-INF/classes/" + controllerPackage.replace('.', '/');
            File directory = new File(getServletContext().getRealPath(path));
            if (directory.exists()) {
                scanDirectory(directory, controllerPackage);
            } else {
                throw new Exception("Directory does not exist: " + directory.getAbsolutePath());
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private void scanDirectory(File directory, String packageName) throws Exception {
        System.out.println("Scanning directory: " + directory.getAbsolutePath());
        try {
            if (directory.listFiles() != null) {

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
                                        Mapping mapping = new Mapping(className, m.getName());
                                        AnnotationGet AnnotationGet = m.getAnnotation(AnnotationGet.class);
                                        String annotationValue = AnnotationGet.value();
                                        if (urlMaping.containsKey(annotationValue)) {
                                            throw new Exception("double url" + annotationValue);
                                        } else {
                                            urlMaping.put(annotationValue, mapping);
                                        }
                                    }
                                }
                                System.out.println("Added controller: " + clazz.getName());
                            }
                        } catch (Exception e) {
                            throw e;
                        }
                    }
                }
            } else {
                throw new Exception("le package est vide");
            }
        } catch (Exception e) {
            throw e;
        }

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
