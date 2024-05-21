package mg.itu.prom16;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

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
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>URL actuelle :</h1>");
            out.println("<p>" + request.getRequestURL() + "</p>");

            out.println("<h2>Liste des contrôleurs annotés avec @AnnotationController :</h2>");
            for (String controller : listeControllers) {
                out.println("<p>" + controller + "</p>");
            }

            StringBuffer requestURL = request.getRequestURL();
            String[] requestUrlSplitted = requestURL.toString().split("/");
            String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];

            if (!urlMaping.containsKey(controllerSearched)) {
                out.println("<p>" + "Aucune methode associee a ce chemin." + "</p>");
            } else {
                out.println("<h2>Classe et methode associe a l'url :</h2>");
                Mapping mapping = urlMaping.get(controllerSearched);

                out.println("<p>" + requestURL.toString() + "</p>");
                out.println("<p>" + mapping.getClassName() + "</p>");
                out.println("<p>" + mapping.getMethodeName() + "</p>");

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
                System.out.println("Directory does not exist: " + directory.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File directory, String packageName) {
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
                                Mapping mapping = new Mapping(className, m.getName());
                                AnnotationGet AnnotationGet = m.getAnnotation(AnnotationGet.class);
                                String annotationValue = AnnotationGet.value();
                                urlMaping.put(annotationValue, mapping);
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
