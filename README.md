# sprint-2380
1-Cree des controllers avec des annotations @AnnotationController(")
2-Dans web.xml declare un parametre "controller-package" comme nom et le chemin vers le package ou se trouve les controllers comme value dans FrontController
3-Annoter les methodes en utilisant AnnotationGet("")
4-dans les controllers, ajouter des fonctions annoter qui retournent des string
5-dans les controllers, instancer des classes ModelAndView avec comme argument le chemin vers le vers le view exemple "/view.jsp" ,puis utiliser la fonction addObject avec comme attributs le nom de l'object a envoyer et le contenu: addObject("nom", "le contenu")