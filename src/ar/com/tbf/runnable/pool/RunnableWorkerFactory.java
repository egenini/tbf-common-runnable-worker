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

import ar.com.tbf.runnable.PooleableRunnableWorker;
import ar.com.tbf.runnable.Worker;
import jodd.props.Props;
import jodd.props.PropsEntry;

public class RunnableWorkerFactory extends BaseKeyedPooledObjectFactory<String, PooleableRunnableWorker>{

	private static final Logger LOG = LoggerFactory.getLogger( RunnableWorkerFactory.class );
	
	private static final Map<String, Constructor> WORKERS_CONSTRUCTORS = new HashMap<String, Constructor>();

	/**
	 * Si se require crear un pool con una clave diferente al nombre de una clase entonces vamos a usar esto para mapear ambos
	 */
	private static final Map<String, String> POOL_KEY_WORKER_NAME_MAPPING = new HashMap<String, String>();

	private static final Map<String, Integer> WORKER_NAME_ID = new HashMap<String, Integer>();

	private static final String PACKAGE_KEY = "workers.package";
	private static final String CLASS_KEY   = "workers.class";

	public static void mapPoolKey2WorkerName( String poolKey, String workerClassName){
		
		WORKER_NAME_ID.put( poolKey , 0 );
		POOL_KEY_WORKER_NAME_MAPPING.put(poolKey, workerClassName);
	}
	
	@Override
	public PooleableRunnableWorker create(String poolKey) throws Exception {
		
		Worker worker = null;
		Integer id = 0;
		
		// esto pasa cuando la key del pool es diferente a la de la clase.
		if( POOL_KEY_WORKER_NAME_MAPPING.containsKey(poolKey) ){
			poolKey = POOL_KEY_WORKER_NAME_MAPPING.get(poolKey);
		}
		
		// si hay un constructor asociado a la key, entonces creamos una instancia.
		if( WORKERS_CONSTRUCTORS.containsKey(poolKey)){
			
			worker = (Worker) WORKERS_CONSTRUCTORS.get(poolKey).newInstance();
		}
		
		if( WORKER_NAME_ID.containsKey(poolKey) ) {
			
			id = WORKER_NAME_ID.get(poolKey) + 1;
			
			WORKER_NAME_ID.put(poolKey, id);
		}
		// si worker está en null es problable que quien use el thread lo establezca
		return new PooleableRunnableWorker(worker).withId(id);
	}

	@Override
	public PooledObject<PooleableRunnableWorker> wrap(PooleableRunnableWorker runnableWorker) {
		return new DefaultPooledObject<PooleableRunnableWorker>(runnableWorker);
	}

	public void destroyObject(final String key, final PooledObject<PooleableRunnableWorker> pooledObject ) throws Exception {
		
		pooledObject.getObject().onDestroy();
	}
	
	public void activateObject( final String poolKey, final PooledObject<PooleableRunnableWorker> pooledObject ) {
		
		pooledObject.getObject().onActivate();
	}

	public void passivateObject( final String poolKey, final PooledObject<PooleableRunnableWorker> pooledObject ) {
		
		pooledObject.getObject().onPassivate();
	}

    @Override
    public boolean validateObject( String poolKey, PooledObject<PooleableRunnableWorker> pooledObject) {
        return pooledObject.getObject().onValidate();
    }
	
	/**
	 * Si no configuramos las clases para el factory le podemos agregar las que necesitemos desde donde sea necesario de forma programática.
	 * 
	 * @param worker
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static void addWorker( Worker worker ) throws NoSuchMethodException, SecurityException{
		
		WORKER_NAME_ID.put( worker.getClass().getName() , 0 );
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

				if( prop.getKey().equals( PACKAGE_KEY ) ){
					
					List<String> names = getClassOfPackage( prop.getValue() );
					
					for( String name : names){
						
						try {
							
							someClass = (Class<Worker>) Class.forName( name );

							WORKER_NAME_ID.put(name, 0);

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
				}else if( prop.getKey().equals( CLASS_KEY ) ){
					
					try {
						
						someClass = (Class<Worker>) Class.forName( prop.getValue() );
						
						WORKER_NAME_ID.put(prop.getValue(), 0);
						
						WORKERS_CONSTRUCTORS.put( prop.getValue(), (Constructor) someClass.getConstructor() );
						
						LOG.info( "Agregando worker constructor para "+ prop.getValue() );
					} catch (ClassNotFoundException e) {
						LOG.error( prop.getValue(),  e );
					} catch (NoSuchMethodException e) {
						LOG.error( prop.getValue(),  e );
					} catch (SecurityException e) {
						LOG.error( prop.getValue(),  e );
					}
					
				}

			}
		}
		return ! WORKERS_CONSTRUCTORS.isEmpty() ;
	}
	
	/**
	 * Busca las clases que pertenecen al paquete.
	 * 
	 * @param packagenom: paquete
	 * @return: Lista de nombre completo de las clases.
	 */
	
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
