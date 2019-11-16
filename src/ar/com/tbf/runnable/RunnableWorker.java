package ar.com.tbf.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.tbf.runnable.pool.RunnableWorkerPool;

/**
 * Es una clase genérica que se ejecuta como un thread, en cada ciclo de su ejecución llama a un método work de otra clase.
 * 
 * Posee comportamiento que sólo se usa cuándo la instancia es PooleableRunnableWorker
 * 
 * @author Edgardo
 *
 */
public class RunnableWorker implements Runnable {

	private Integer id;
	private Logger  log        = LoggerFactory.getLogger(RunnableWorker.class);
	private boolean shutdown   = false;
	private long    waitingFor = 8 * 60 * 60 * 1000;
	private Thread  thread;
	protected Worker  worker     = null;
	private boolean wakeUp     = false;
	protected Exception error = null;
	// este atributo tiene valor true si se instancia PooleableRunnableWorker
	protected boolean pooleable = false;
	
	// thread times
	private long wakeUpTime  = 0;
	protected long posWorkTime = 0;
	
	/**
	 * Este constructor es para usar en pool, duerme al thread indefinidamente hasta que quien lo use lo despierte.
	 *  
	 * @param worker
	 */
	public RunnableWorker( Worker worker ){

		this( worker, -1);
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
							
							// si usa pool es en este momento que considera si es oportuno volver.
							this.returnToPool();
							this.wait();
						}
						else{
							this.wait( waitingFor );
						}
					}
				} catch (InterruptedException e) {
				}
			}
			
			wakeUpTime = 0;
			error      = null;
			
			if( ! this.isShutdown() ){

				if( worker != null ){
					
					try {
						
						wakeUpTime = System.currentTimeMillis();
						
						worker.work();
						
					}catch(Exception e ) {
						
						error = e;
					}
					finally {
						
						postWork();
					}
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
	
	public Worker getWorker() {
		return worker;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}
	
	/**
	 * Este método es sobreescrito por PooleableRunnableWorker para retornar el objeto al pool.
	 * 
	 */
	protected void returnToPool() {
		
	}

	public void postWork() {
		
		this.posWorkTime = System.currentTimeMillis();
	}


	public String printStatus() {
		
		return "{ \"has_error\": "+ (this.error != null ? "true" : "false") +", \"wakeUpTime\": "+ this.wakeUpTime +", \"posWorkTime\":"+ this.posWorkTime +"}";
	}

	public Integer getId() {
		return id;
	}

	public RunnableWorker setId(Integer id) {
		this.id = id;
		return this;
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

	public long getWakeUpTime() {
		return wakeUpTime;
	}

	public void setWakeUpTime(long wakeUpTime) {
		this.wakeUpTime = wakeUpTime;
	}

	public long getPosWorkTime() {
		return posWorkTime;
	}

	public void setPosWorkTime(long posWorkTime) {
		this.posWorkTime = posWorkTime;
	}
	
}
