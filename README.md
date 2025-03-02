# fun with libraries

This codebase demonstrates how:

* to create a custom starter with autoconfig to help custom contributors, provide the types they need, etc.
* a custom contributor ought to structure their contribution itself as an autoconfig
* to intelligently 'discover' things for these plugins
* to ensure these plugins have what they need during compilation

The `flow-starter` provides required types for plugin contribution developers. It also provides autoconfiguration for
all components on the classpath,
discovering the `Flow` class, and resolving their required `${Flow#flowName}.properties` configuration files.

The `flow-service` is a standin for any ol' Spring Boot application, wherein all contributed modules are amassed.

The `github-flow-definition-starter` is a (basically empty) example of a simple contribution. In this case, it's one
that simply runs and prints out a message loaded from that plugin's property file

## technical limitations
The problem is that everything gets added to a global  `org.springframework.core.env.Environment`.
It's a flat, global namespace. If one plugin defines  `spring.datasource.username`  (for instance), and then another
module defines it, they'll end up clobbering each other and, worse, the value defined in the `flow-service` itself! 

