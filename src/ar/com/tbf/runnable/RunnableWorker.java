package ar.com.tbf.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Es una clase genérica que se ejecuta como un thread, en cada ciclo de su ejecución llama a un método work de otra clase.
 * 
 * @author Edgardo
 *
 */
public class RunnableWorker implements Runnable {

	private Logger log = LoggerFactory.getLogger(RunnableWorker.class);
	
	private boolean shutdown   = false;
	private long    waitingFor = 8 * 60 * 60 * 1000;
	private Thread  thread;
	private Worker  worker = null;
	private boolean wakeUp = false;
	/**
	 * Este constructor es para usar en pool, duerme al thread indefinidamente hasta que quien lo use lo despierte.
	 *  
	 * @param worker
	 */
	public RunnableWorker( Worker worker ){

		this.waitingFor = -1; // inidca que va a dormir de forma indefinida.
		this.worker = worker;
	}
	
	public RunnableWorker( Worker worker, long timeToWait ){
		
		this.waitingFor = timeToWait;
		this.worker     = worker;
		
		log.info( "El thead espera " + this.waitingFor +" ms para ejecutar" );
		
	}

	/**
	 * Crea el thread y lo lanza, espera el tiempo definido antes de invocar al worker, si se requiere que ejecute antes se puede invocar a wakeUp.
	 * 
	 */
	public void excecute(){

		if( worker == null ){
			
			log.error( "No se estableció ninguna clase para que haga trabajo alguno, este thread va correr inútilmente a menos que se agregue un worker usando el método setWorker" );
		}
		
		if(this.thread == null){
			
			this.thread = new Thread(this);

			this.thread.start();
		}
		
	}

	@Override
	public void run() {

		while( ! this.isShutdown() ){

			if( ! wakeUp ){
				
				try {
					synchronized (this) {
						if( this.waitingFor == -1 ){
							this.wait();
						}
						else{
							this.wait( waitingFor );
						}
					}
				} catch (InterruptedException e) {
				}
			}

			if( ! this.isShutdown() ){

				if( worker != null ){
					worker.work();
				}
				wakeUp = false;
			}
		}
	}

	/**
	 * Despierta al thread.
	 */
	public synchronized void wakeUp(){
		
		this.wakeUp = true;
		
		// si no instanció el thread aún entonces execute lo hace y el notify hace que salga de la espera y ejecute ahora.
		this.excecute();
		
		this.notify();
	}

	public synchronized void shutdown(){
		
		this.worker.releaseResources();
		this.setShutdown(true);
	}
	
	public boolean isShutdown() {
		return shutdown;
	}

	public synchronized void setShutdown(boolean shutdown) {
		this.shutdown = shutdown;
		this.notify();
	}

	public long getWaitingFor() {
		return waitingFor;
	}

	public void setWaitingFor(long waitingFor) {
		this.waitingFor = waitingFor;
	}

	public Worker getWorker() {
		return worker;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}
}
