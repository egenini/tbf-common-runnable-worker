package ar.com.tbf.runnable.pool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import ar.com.tbf.runnable.RunnableWorker;
import jodd.bean.BeanUtil;
import jodd.props.Props;
import jodd.props.PropsEntry;

public class RunnableWorkerPool {

	private KeyedObjectPool<String, RunnableWorker> pool;

    private static class SingletonHolder {

    	public static final RunnableWorkerPool INSTANCE = new RunnableWorkerPool();
    }

    public static RunnableWorkerPool getInstance() {
            return SingletonHolder.INSTANCE;
    }

    private RunnableWorkerPool()
    {
    	InputStream is = null;
    	
    	Props p = new Props();
        try {
        	
        	is = RunnableWorkerPool.class.getClassLoader().getResourceAsStream("RunnableWorkerPool.props");
			
        	if( is != null ){
        		
            	p.load(is);
    			
    			RunnableWorkerFactory.buildWorkers(p);
    			
    			// buscamos si hay alias a los pools

    			startPool( p);
        	}
			
		} catch (IOException e) {
		}finally{
			if( is != null){
				try{
					is.close();
				}catch(Exception e){}
			}
		}
        
    }

    /**
     * 
     * @param objectName: Clave/Nombre del pool de donde obtiene el objeto (RunnableWorker)
     * @return
     * @throws NoSuchElementException
     * @throws IllegalStateException
     * @throws Exception
     */
    public Object getObject( String objectName ) throws NoSuchElementException, IllegalStateException, Exception{
    	
    	return this.getPool().borrowObject(objectName);
    }

    /**
     * Devuelve el RunnableWorker al pool.
     * @param poolKey: Clave/Nombre del pool.
     * @param runnableWorker
     * @throws Exception
     */
	public void returnObject(String poolKey, RunnableWorker runnableWorker) throws Exception {

		this.getPool().returnObject(poolKey, runnableWorker);
	}

	/**
     * 
     * @return the org.apache.commons.pool.KeyedObjectPool class
     */
    public KeyedObjectPool<String, RunnableWorker> getPool() {
            return pool;
    }

    /**
     * 
     * @return the org.apache.commons.pool.KeyedObjectPool class
     */
    public void startPool(Props p) {
    	
    	GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
    	
    	Iterator<PropsEntry> it = p.entries()
                .section("pool")
                .iterator();
    	PropsEntry prop;
    	
    	while( it.hasNext() ){
    		
    		prop = it.next();
    		
    		if( BeanUtil.pojo.hasProperty(config, prop.getKey() )){
    			
    			BeanUtil.pojo.setProperty(config, prop.getKey(), prop.getValue() );
    		}
    	}

    	this.startPool(config);
    }
    
    public void startPool(GenericKeyedObjectPoolConfig config ) {
    
    	pool =  new GenericKeyedObjectPool(new RunnableWorkerFactory(),config);
    }
	public static void main(String[] args) {

    	Props p = new Props();
    	p.setAppendDuplicateProps(true);

    	try {
        	InputStream is = RunnableWorkerPool.class.getClassLoader().getResourceAsStream("RunnableWorkerPool_template.props");
        	
			p.load(is);
			
		} catch (Exception e) {

			try {
				p.load(new File("C:\\wrokspace_oxygen\\RunnableWorker\\src\\ar\\com\\tbf\\runnable\\pool\\RunnableWorkerPool_template.props"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

    	System.out.println(p.getValue("services.package"));
    	System.out.println("**************");
    	Iterator<PropsEntry> it = p.entries()
                .section("services")
                .iterator();
    	PropsEntry prop;
    	
    	while( it.hasNext() ){
    		
    		prop = it.next();

    		System.out.println( prop.getKey() +" "+ prop.getValue() );
    	}
		
		/*
		try {
			ServiceTest service = (ServiceTest)ServicePool.getInstance().getPool().borrowObject("ServiceTest");
			
			System.out.println("Elementos en el pool: "+ ServicePool.getInstance().getPool().getNumIdle());
			System.out.println("Obtenido del pool: "+ service.getClass().getSimpleName());
			
			ServicePool.getInstance().getPool().returnObject("ServiceTest", service);
			System.out.println("Elementos en el pool: "+ ServicePool.getInstance().getPool().getNumIdle());
			
			
		} catch (NoSuchElementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}

}
