package ar.com.tbf.runnable.pool;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.ClassPath;

import ar.com.tbf.runnable.RunnableWorker;
import ar.com.tbf.runnable.Worker;
import jodd.props.Props;
import jodd.props.PropsEntry;

public class RunnableWorkerFactory extends BaseKeyedPooledObjectFactory<String, RunnableWorker>{

	private static final Logger LOG = LoggerFactory.getLogger( RunnableWorkerFactory.class );
	
	private static final Map<String, Constructor> WORKERS_CONSTRUCTORS = new HashMap<String, Constructor>();

	/**
	 * Si se require crear un pool con una clave diferente al nombre de una clase entonces vamos a usar esto para mapear ambos
	 */
	private static final Map<String, String> POOL_KEY_WORKER_NAME_MAPPING = new HashMap<String, String>();

	public static void mapPoolKey2WorkerName( String poolKey, String workerClassName){
		
		POOL_KEY_WORKER_NAME_MAPPING.put(poolKey, workerClassName);
	}
	
	@Override
	public RunnableWorker create(String key) throws Exception {
		
		Worker worker = null;
		
		// esto pasa cuando la key del pool es diferente a la de la clase.
		if( POOL_KEY_WORKER_NAME_MAPPING.containsKey(key) ){
			key = POOL_KEY_WORKER_NAME_MAPPING.get(key);
		}
		
		// si hay un constructor asociado a la key, entonces creamos una instancia.
		if( WORKERS_CONSTRUCTORS.containsKey(key)){
			
			worker = (Worker) WORKERS_CONSTRUCTORS.get(key).newInstance();
		}
		
		// si worker está en null es problable que quien use el thread lo setee
		return new RunnableWorker(worker);
	}

	@Override
	public PooledObject<RunnableWorker> wrap(RunnableWorker value) {
		return new DefaultPooledObject(value);
	}

	/**
	 * Si no configuramos las clases para el factory le podemos agregar las que necesitemos desde donde sea necesario de forma programática.
	 * 
	 * @param worker
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static void addWorker( Worker worker ) throws NoSuchMethodException, SecurityException{
		
		WORKERS_CONSTRUCTORS.put(worker.getClass().getName(), (Constructor) worker.getClass().getConstructor());

	}
	/**
	 * Retorna true si existe al menos 1 constructor configurado
	 * @param props
	 * @return
	 */
	public static boolean buildWorkers( Props props ){
		
		Class<Worker> someClass;
		
		Iterator<PropsEntry> it = props.entries().section("workers").iterator();
    	PropsEntry prop;

		while( it.hasNext() ){

			prop = it.next();
			
			if( prop.getValue() != null ){
				
				List<String> names = getClassOfPackage(prop.getValue());
				
				for( String name : names){
					
					try {
						
						someClass = (Class<Worker>) Class.forName( name );
						
						WORKERS_CONSTRUCTORS.put(name, (Constructor) someClass.getConstructor());
						
						LOG.info( "Agregando worker constructor para "+ name );
					} catch (ClassNotFoundException e) {
						LOG.error( name,  e );
					} catch (NoSuchMethodException e) {
						LOG.error( name,  e );
					} catch (SecurityException e) {
						LOG.error( name,  e );
					}
				}
			}
		}
		return ! WORKERS_CONSTRUCTORS.isEmpty() ;
	}
	
	private static List<String> getClassOfPackage(String packagenom) {

	    final ClassLoader loader = Thread.currentThread().getContextClassLoader();
	    
	    List<String> classes = new ArrayList<String>();
	    
	    try {

	    	ClassPath classpath = ClassPath.from(loader); // scans the class path used by classloader
	    	for (ClassPath.ClassInfo classInfo : classpath.getTopLevelClasses(packagenom)) {

	    		if(!classInfo.getSimpleName().endsWith("_")){

	    			classes.add(classInfo.getName());
	    		}
	    	}
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    return classes;
	}
}
