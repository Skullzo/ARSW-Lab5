
### Escuela Colombiana de Ingeniería

### Arquitecturas de Software


## Laboratorio API REST para la gestión de planos

### Dependencias
* [Laboratorio Componentes y conectores Middleware- gestión de planos (Blueprints) Parte 1](https://github.com/ARSW-ECI-beta/REST_API-JAVA-BLUEPRINTS_PART1)

### Descripción
En este ejercicio se va a construír el componente BlueprintsRESTAPI, el cual permita gestionar los planos arquitectónicos de una prestigiosa compañia de diseño. La idea de este API es ofrecer un medio estandarizado e 'independiente de la plataforma' para que las herramientas que se desarrollen a futuro para la compañía puedan gestionar los planos de forma centralizada.
El siguiente, es el diagrama de componentes que corresponde a las decisiones arquitectónicas planteadas al inicio del proyecto:

![](img/CompDiag.png)

Donde se definió que:

* El componente BlueprintsRESTAPI debe resolver los servicios de su interfaz a través de un componente de servicios, el cual -a su vez- estará asociado con un componente que provea el esquema de persistencia. Es decir, se quiere un bajo acoplamiento entre el API, la implementación de los servicios, y el esquema de persistencia usado por los mismos.

Del anterior diagrama de componentes (de alto nivel), se desprendió el siguiente diseño detallado, cuando se decidió que el API estará implementado usando el esquema de inyección de dependencias de Spring (el cual requiere aplicar el principio de Inversión de Dependencias), la extensión SpringMVC para definir los servicios REST, y SpringBoot para la configurar la aplicación:


![](img/ClassDiagram.png)

### Parte I

1. Integre al proyecto base suministrado los Beans desarrollados en el ejercicio anterior. Sólo copie las clases, NO los archivos de configuración. Rectifique que se tenga correctamente configurado el esquema de inyección de dependencias con las anotaciones @Service y @Autowired.

**Para el proyecto base, implementamos los métodos ya implementados en el ejercicio anterior, que son ```BlueprintServices```, ```BlueprintsFilter```, ```RedundancyFilter```, ```SubsamplingFilter```, entre otras. Para esto, primero implementamos la clase ```BlueprintServices```, con su respectivo esquema de inyección de dependencias con las anotaciones ```@Service``` y ```@Autowired```, quedando de la siguiente forma.**

```java
@Service("BlueprintsServices")
public class BlueprintsServices {
    @Autowired
    @Qualifier("InMemoryBlueprintPersistence")
    BlueprintsPersistence bpp;
    @Autowired
    @Qualifier("RedundancyFilter")
    BlueprintsFilter bpf;
    public Blueprint filter(Blueprint bp){
        return bpf.filter(bp);
    }
    public void addNewBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        bpp.saveBlueprint(bp);
    } 
    public Set<Blueprint> getAllBlueprints() throws BlueprintNotFoundException {
        return bpp.getAllBlueprints();
    }  
    /**
     * 
     * @param author blueprint's author
     * @param name blueprint's name
     * @return the blueprint of the given name created by the given author
     * @throws BlueprintNotFoundException if there is no such blueprint
     */
    public Blueprint getBlueprint(String author,String name) throws BlueprintNotFoundException{
        return bpp.getBlueprint(author,name);
    }   
    /**
     * 
     * @param author blueprint's author
     * @return all the blueprints of the given author
     * @throws BlueprintNotFoundException if the given author doesn't exist
     */
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException{
        return bpp.getBlueprintsByAuthor(author);
    }

    public void updateBlueprint(Blueprint bp,String author,String name) throws BlueprintNotFoundException {
        bpp.updateBlueprint(bp,author,name);
    }
}
```

2. Modifique el bean de persistecia 'InMemoryBlueprintPersistence' para que por defecto se inicialice con al menos otros tres planos, y con dos asociados a un mismo autor.

**A continuación se realiza la siguiente modificación al bean de persistecia 'InMemoryBlueprintPersistence', en el cual se crean tres planos, dos asociados a un mismo autor (David) y otro plano asociado a un autor (Alejandro), quedando la clase de la siguiente forma.**

```java
@Service("InMemoryBlueprintPersistence")
public class InMemoryBlueprintPersistence implements BlueprintsPersistence{
    private final ConcurrentHashMap<Tuple<String,String>,Blueprint> blueprints=new ConcurrentHashMap<>();
    public InMemoryBlueprintPersistence() {
        Point[] pts=new Point[]{new Point(140, 140),new Point(115, 115)};
        Point[] points= new Point[] {new Point(1,2),new Point(3,4),new Point(1,2)};
        Point[] pts2=new Point[]{new Point(14, 14),new Point(11, 15)};
        Blueprint bp=new Blueprint("Alejandro", "bp1",pts);
        Blueprint bp2=new Blueprint("David","bp2",points);
        Blueprint bp3=new Blueprint("David","bp3",pts2);
        blueprints.put(new Tuple<>(bp.getAuthor(),bp.getName()), bp);
        blueprints.put(new Tuple<>(bp2.getAuthor(),bp2.getName()), bp2);
        blueprints.put(new Tuple<>(bp3.getAuthor(),bp3.getName()), bp3);
    }     
    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        Blueprint blueprint= blueprints.putIfAbsent(new Tuple<>(bp.getAuthor(),bp.getName()), bp);
        if (blueprint!=null){
            throw new BlueprintPersistenceException("The given blueprint already exists: "+bp);
        }
    }
    @Override
    public  HashSet<Blueprint> getAllBlueprints(){
        return new HashSet<Blueprint>(blueprints.values());
    }
    @Override
    public void updateBlueprint(Blueprint bp,String author,String name) throws BlueprintNotFoundException {
        Blueprint oldbp=getBlueprint(author,name);
        oldbp.setPoints(bp.getPoints());
    }
    @Override
    public Blueprint getBlueprint(String author, String bprintname) throws BlueprintNotFoundException {
        Blueprint bp=blueprints.get(new Tuple<>(author, bprintname));
        if(bp==null)throw new BlueprintNotFoundException("El plano con estas caracteristicas no existe");
        return bp;
    }
    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException{
        Set<Blueprint> ans = new HashSet<>();
        for(Map.Entry<Tuple<String, String>, Blueprint> i :blueprints.entrySet()){
            if(i.getKey().o1.equals(author)){
                ans.add(i.getValue());
            }
        }
        if(ans.size()==0) throw new BlueprintNotFoundException("Este usuario no tiene planos");
        return ans;
    }
}
```

3. Configure su aplicación para que ofrezca el recurso "/blueprints", de manera que cuando se le haga una petición GET, retorne -en formato jSON- el conjunto de todos los planos. Para esto:

	* Modifique la clase BlueprintAPIController teniendo en cuenta el siguiente ejemplo de controlador REST hecho con SpringMVC/SpringBoot:

	```java
	@RestController
	@RequestMapping(value = "/url-raiz-recurso")
	public class XXController {
    
        
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> manejadorGetRecursoXX(){
        try {
            //obtener datos que se enviarán a través del API
            return new ResponseEntity<>(data,HttpStatus.ACCEPTED);
        } catch (XXException ex) {
            Logger.getLogger(XXController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>("Error bla bla bla",HttpStatus.NOT_FOUND);
        }        
	}

	```
	* Haga que en esta misma clase se inyecte el bean de tipo BlueprintServices (al cual, a su vez, se le inyectarán sus dependencias de persisntecia y de filtrado de puntos).

**A continuación, se agrega a la clase las anotaciones ```@RestController``` y ```@RequestMapping```, en el cual se agrega el recurso ```/blueprints``` como se pide en el enunciado del problema. Asimismo, se agrega la respectiva inyección del bean de tipo ```BlueprintServices```, al cual se le realizan las respectivas inyecciones de sus dependencias de persisntecia y de filtrado de puntos, mediante anotaciones como ```@Autowired``` y ```@Qualifier```. También se completa la implementación de la excepción agregando un error si no se encuentra la página, y retornando la página si se encuentran los planos.**

```java
@RestController
@RequestMapping(value = "/blueprints")
public class BlueprintAPIController {

    @Autowired
    @Qualifier("BlueprintsServices")
    BlueprintsServices services;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> manejadorBlueprints(){

        try {
            //obtener datos que se enviarán a través del API
            return new ResponseEntity<>(services.getAllBlueprints(), HttpStatus.ACCEPTED);
        } catch (Exception ex) {
            Logger.getLogger(BlueprintAPIController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>("Error bla bla bla",HttpStatus.NOT_FOUND);
        }
    }
}
```

4. Verifique el funcionamiento de a aplicación lanzando la aplicación con maven:

	```bash
	$ mvn compile
	$ mvn spring-boot:run
	
	```
	Y luego enviando una petición GET a: http://localhost:8080/blueprints. Rectifique que, como respuesta, se obtenga un objeto jSON con una lista que contenga el detalle de los planos suministados por defecto, y que se haya aplicado el filtrado de puntos correspondiente.
	
**Primero, para comprobar que mas planos sirvieran antes de realizar el experimento, se realizó la siguiente modificación a la clase o persistence bean ```InMemoryBlueprintPersistence```, quedando de la siguiente forma.**

```java
public InMemoryBlueprintPersistence() {
    	Point[] pts=new Point[]{new Point(140, 140),new Point(115, 115)};
        Blueprint bp1=new Blueprint("carlos", "prueba",pts);
        Blueprint bp2=new Blueprint("negro", "obra2",pts);
        Blueprint bp3=new Blueprint("juan", "iliada",pts);
        Blueprint bp4=new Blueprint("juan", "SDFSDF",pts);
        Blueprint bp5=new Blueprint("negro", "aasda",pts);
        blueprints.put(new Tuple<>(bp1.getAuthor(),bp1.getName()), bp1);
        blueprints.put(new Tuple<>(bp2.getAuthor(),bp2.getName()), bp2);
        blueprints.put(new Tuple<>(bp3.getAuthor(),bp3.getName()), bp3);
        blueprints.put(new Tuple<>(bp4.getAuthor(),bp4.getName()), bp4);
        blueprints.put(new Tuple<>(bp5.getAuthor(),bp5.getName()), bp5);
    }   
```

**Luego de realizar una petición GET a http://localhost:8080/blueprints, luego de haber ejecutado los comandos ```mvn compile``` y ```mvn spring-boot:run``` respectivamente, se obtiene un objeto JSON con la lista que contiene el detalle de los planos suministados por defecto, con sus respectivos filtros. Al escribir en el navegador ```localhost:8080/blueprints```, se obtiene el siguiente resultado.**

![img](https://github.com/Skullzo/ARSW-Lab5/blob/main/img/Parte1.4.png)

5. Modifique el controlador para que ahora, acepte peticiones GET al recurso /blueprints/{author}, el cual retorne usando una representación jSON todos los planos realizados por el autor cuyo nombre sea {author}. Si no existe dicho autor, se debe responder con el código de error HTTP 404. Para esto, revise en [la documentación de Spring](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html), sección 22.3.2, el uso de @PathVariable. De nuevo, verifique que al hacer una petición GET -por ejemplo- a recurso http://localhost:8080/blueprints/juan, se obtenga en formato jSON el conjunto de planos asociados al autor 'juan' (ajuste esto a los nombres de autor usados en el punto 2).

**A contnuación, se modifica el controlador en la clase correspondiente, que es ```BlueprintAPIController```, en la cual ahora se encarga de aceptar peticiones GET al recurso /blueprints/{author}, y al realizarle la petición GET, este retorna usando una representación jSON todos los planos realizados por el autor cuyo nombre es {author}. Para esto se tomó el mismo ejemplo descrito en el enunciado, que es ```http://localhost:8080/blueprints/juan```.**

```java
@RequestMapping(value="/{author}", method = RequestMethod.GET)
public ResponseEntity<?>  manejadorBlueprintsByAuthor(@PathVariable("author") String author){

        try {
            //obtener datos que se enviarán a través del API
            return new ResponseEntity<>(services.getBlueprintsByAuthor(author), HttpStatus.ACCEPTED);
        } catch (Exception ex) {
            Logger.getLogger(BlueprintAPIController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
}
```

**En la siguiente imgen se encuentra en formato JSON el conjunto de planos asociados al autor 'juan', luego de escribir en el navegador ```http://localhost:8080/blueprints/juan```.**

![img](https://github.com/Skullzo/ARSW-Lab5/blob/main/img/Parte1.5.png)

6. Modifique el controlador para que ahora, acepte peticiones GET al recurso /blueprints/{author}/{bpname}, el cual retorne usando una representación jSON sólo UN plano, en este caso el realizado por {author} y cuyo nombre sea {bpname}. De nuevo, si no existe dicho autor, se debe responder con el código de error HTTP 404. 

**Ahora, nuevamente modificamos el controlador en la clase correspondiente, que es ```BlueprintAPIController```, el cual ahora acepta peticiones GET al recurso /blueprints/{author}/{bpname}, el cual retorna una representación jSON sólo UN plano, en este caso el realizado por {author} y cuyo nombre sea {bpname}. Las modificaciones realizadas se ven en el siguiente código.**

```java
@RequestMapping(value="/{author}/{name}", method = RequestMethod.GET)
public ResponseEntity<?>  manejadorBlueprint(@PathVariable("author") String author,@PathVariable("name") String name ){
        try {
            //obtener datos que se enviarán a través del API
            return new ResponseEntity<>(services.getBlueprint(author,name), HttpStatus.ACCEPTED);
        } catch (Exception ex) {
            Logger.getLogger(BlueprintAPIController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
}
```

**Al ingresar el URL correspondiente en el navegador, que es ```http://localhost:8080/blueprints/juan/iliada```, en el cual se encuentra el autor (juan) y el nombre (iliada). El resultado obtenido se puede observar a continuación.**

![img](https://github.com/Skullzo/ARSW-Lab5/blob/main/img/Parte1.6.png)

-----------------------------------------------------------------------------------

### Parte II

1.  Agregue el manejo de peticiones POST (creación de nuevos planos), de manera que un cliente http pueda registrar una nueva orden haciendo una petición POST al recurso ‘planos’, y enviando como contenido de la petición todo el detalle de dicho recurso a través de un documento jSON. Para esto, tenga en cuenta el siguiente ejemplo, que considera -por consistencia con el protocolo HTTP- el manejo de códigos de estados HTTP (en caso de éxito o error):

	```	java
	@RequestMapping(method = RequestMethod.POST)	
	public ResponseEntity<?> manejadorPostRecursoXX(@RequestBody TipoXX o){
        try {
            //registrar dato
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (XXException ex) {
            Logger.getLogger(XXController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>("Error bla bla bla",HttpStatus.FORBIDDEN);            
        }        
 	
	}
	```	

**A continuación, se agrega en la clase ```BlueprintAPIController``` el mandejo de peticiones POST, la cual se encarga de crear nuevos planos, en la cual un cliente HTTP pueda registrar una nueva orden haciendo una petición POST al recurso ‘planos’, en el cual se envia como contenido de la petición todo el detalle de dicho recurso a través de un documento jSON. Las modificaciones del código se observan a continuación.**

```java
@RequestMapping(value="/crear-blueprint",method = RequestMethod.POST)
@ResponseBody
public ResponseEntity<?> manejadorPostBlueprint(@RequestBody Blueprint bp){
        try {
            services.addNewBlueprint(bp);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception ex) {
            Logger.getLogger(BlueprintAPIController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>("Error bla bla bla",HttpStatus.FORBIDDEN);
        }
}
```

2.  Para probar que el recurso ‘planos’ acepta e interpreta
    correctamente las peticiones POST, use el comando curl de Unix. Este
    comando tiene como parámetro el tipo de contenido manejado (en este
    caso jSON), y el ‘cuerpo del mensaje’ que irá con la petición, lo
    cual en este caso debe ser un documento jSON equivalente a la clase
    Cliente (donde en lugar de {ObjetoJSON}, se usará un objeto jSON correspondiente a una nueva orden:

	```	
	$ curl -i -X POST -HContent-Type:application/json -HAccept:application/json http://URL_del_recurso_ordenes -d '{ObjetoJSON}'
	```	

	Con lo anterior, registre un nuevo plano (para 'diseñar' un objeto jSON, puede usar [esta herramienta](http://www.jsoneditoronline.org/)):
	

	Nota: puede basarse en el formato jSON mostrado en el navegador al consultar una orden con el método GET.
	
**Ahora, se realiza la respectiva prueba del recurso ‘planos’, el cual acepta e interpreta correctamente las peticiones POST. Para esto se utiliza el comando curl de Unix que se encuentra en el enunciado, pero en este se realizan las siguientes modificaciones a continuación.**

```
curl -i -X POST -HContent-Type:application/json -HAccept:application/json http://localhost:8080/blueprints -d "{"""author""":"""checho""","""points""":[{"""x""":10,"""y""":10},{"""x""":15,"""y""":0}],"""name""":"""obra"""}"
```

3. Teniendo en cuenta el autor y numbre del plano registrado, verifique que el mismo se pueda obtener mediante una petición GET al recurso '/blueprints/{author}/{bpname}' correspondiente.

**Teniendo en cuenta el autor establecido en el punto 2, que es ```checho```, y el nombre, que es ```obra```, los cuales fueron ingresados en el comando curl de Unix, se procede a realizar la verificación en el que el mismo se pueda obtener mediante una petición GET al recurso '/blueprints/{author}/{bpname}' correspondiente. Para esto, se ingresa en el navegador la URL que es ```http://localhost:8080/blueprints/checho/obra```, y el resultado obtenido es el siguiente.**

![img](https://github.com/Skullzo/ARSW-Lab5/blob/main/img/Parte2.3.png)

4. Agregue soporte al verbo PUT para los recursos de la forma '/blueprints/{author}/{bpname}', de manera que sea posible actualizar un plano determinado.

**A continuación, se agrega el respectivo soporte al verbo PUT para los recursos de la forma '/blueprints/{author}/{bpname}', en la cual es posible actualizar un plano determinado. Para esto, se realizan las respectivas modificaciones a la clase ```BlueprintAPIController```, quedando de la siguiente forma.**

```java
@RequestMapping(value="/{author}/{name}",method = RequestMethod.PUT)
@ResponseBody
public ResponseEntity<?> manejadorPutBlueprint(@PathVariable("author") String author,@PathVariable("name") String name,@RequestBody Blueprint bp ) {
        try {
            services.updateBlueprint(bp,author,name);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception ex) {
            Logger.getLogger(BlueprintAPIController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>( HttpStatus.NOT_FOUND);
        }
}
```

-----------------------------------------------------------------------------------

### Parte III

El componente BlueprintsRESTAPI funcionará en un entorno concurrente. Es decir, atederá múltiples peticiones simultáneamente (con el stack de aplicaciones usado, dichas peticiones se atenderán por defecto a través múltiples de hilos). Dado lo anterior, debe hacer una revisión de su API (una vez funcione), e identificar:

* Qué condiciones de carrera se podrían presentar?
* Cuales son las respectivas regiones críticas?

Ajuste el código para suprimir las condiciones de carrera. Tengan en cuenta que simplemente sincronizar el acceso a las operaciones de persistencia/consulta DEGRADARÁ SIGNIFICATIVAMENTE el desempeño de API, por lo cual se deben buscar estrategias alternativas.

Escriba su análisis y la solución aplicada en el archivo ANALISIS_CONCURRENCIA.txt


## Autores
[Alejandro Toro Daza](https://github.com/Skullzo)

[David Fernando Rivera Vargas](https://github.com/DavidRiveraRvD)
## Licencia & Derechos de Autor
**©** Alejandro Toro Daza, David Fernando Rivera Vargas. Estudiantes de Ingeniería de Sistemas de la [Escuela Colombiana de Ingeniería Julio Garavito](https://www.escuelaing.edu.co/es/).

Licencia bajo la [GNU General Public License](https://github.com/Skullzo/ARSW-Lab5/blob/main/LICENSE).
