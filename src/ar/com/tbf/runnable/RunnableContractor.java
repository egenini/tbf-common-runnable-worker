package ar.com.tbf.runnable;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread que al cumplirse la condición ejecuta invocando a worker.work()
 * 
 * El thread una vez iniciado con start entra en un bucle que sale con shutdown, una vez en el buble:
 * si tiene definido un waitingFor espera el tiempo antes de ejecutar.
 * si se definió que debe esperar indefinidamente entonces deberá usar wakeUp() que se ejecute.
 * 
 * si no se definió una espera es lo mismo que haber definido una ejecución por única vez.
 * 
 * 
 * @author Edgardo
 *
 */
public class RunnableContractor implements Runnable {

	private static final int STAKEHOLDER__OBJECT = 0;
	private static final int STAKEHOLDER__START = 1;
	private static final int STAKEHOLDER__END = 2;
	
	private static final Long PERPETUAL = -1l;
	
	private Logger  log = LoggerFactory.getLogger(RunnableWorker.class);
	private Thread  thread;
	private Worker  worker = null;
	private Long    waitingFor = null;
	private boolean shutdown = false;
	private List<List<Object>> stakeholders = new ArrayList<List<Object>>();
	/**
	 * Constructor especial para usar con el estilo de encadenar métodos (withM...)
	 * 
	 * Ej: de una ejecución inmediata mediante wakeUp, sino espera y luego ejecuta.
	 * RunnableContractor runnableContractor = RunnableContractor builder().withWorker(new myWorker ).withWaitingFor( 50000 ).wakeUp();
	 * 
	 * RunnableContractor runnableContractor = RunnableContractor builder().withStakeholder( stakeholder ).withWaitingFor( 50000 ).wakeUp();

	 * Ej: de una ejecución por única vez.
	 * RunnableContractor runnableContractor = RunnableContractor builder().withWorker(new myWorker ).withWaitingFor( 50000 ).asOneRun();

	 * Ej: de una espera perpetua.
	 * RunnableContractor runnableContractor = RunnableContractor builder().withWorker(new myWorker ).withWaitingForever();

	 * @return
	 */
	public static RunnableContractor builder(){
	
		return new RunnableContractor();
	}

	/**
	 * Establece que el thread duerme hasta que se lo despierte mediante wakeUp()
	 * 
	 * @return
	 */
	public RunnableContractor withWaitingForever(){
		
		this.waitingFor = PERPETUAL;
		
		return this;
	}

	/**
	 * Establece shutdown en true lo que provoca la ejecución por única vez del thread.
	 * 
	 * @return
	 */
	public RunnableContractor asOneRun(){
		
		this.shutdown = true;
		
		return this;
	}
	
	public RunnableContractor withWorker( Worker  worker){
		
		this.worker = worker;
		
		return this;
	}

	/**
	 * Agrega a una lista al interesado, a este se le invocarán los métodos (si los tiene) cuando comience el trabajo y cuando termine.
	 * 
	 * @param stakeholder
	 * @return
	 */
	public RunnableContractor withStakeholder( Object stakeholder, String methodWhenStart, String methodWhenEnd){
		
		ArrayList<Object> params = new ArrayList<Object>( 3 );
		
		params.add(stakeholder);
		params.add(methodWhenStart);
		params.add(methodWhenEnd);
		
		this.stakeholders.add( params);
		
		return this;
	}

	/**
	 * Quita al objeto de la lista
	 * @param stakeholder
	 * @return
	 */
	public RunnableContractor withoutStakeholder( Object stakeholder){
		
		this.stakeholders.remove(stakeholder);
		
		return this;
	}

	public RunnableContractor withWaitingFor( Long  waitingFor ){
		
		this.waitingFor = waitingFor;
		
		return this;
	}
	
	public synchronized void shutdown(){
	
		this.shutdown = true;
		this.notify();
	}
	
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
	
		do{
			if( this.waitingFor != null ){
				try {
					synchronized (this) {
						if( this.waitingFor == PERPETUAL ){
							log.isInfoEnabled(); log.info("En espera indeterminada");
							this.wait();
						}
						else{
							log.isInfoEnabled(); log.info("En espera por "+ waitingFor);
							this.wait( waitingFor );
						}
					}
				} catch (InterruptedException e) {
				}
			}

			notifyStakeholders(true);
			
			log.isInfoEnabled(); log.info("Worker "+ worker.getClass().getName() +" comienza a trabajar ");
			
			worker.work();

			log.isInfoEnabled(); log.info("Worker "+ worker.getClass().getName() +" terminó de trabajar ");

			notifyStakeholders(false);

		}while( ! this.shutdown );
	}

	protected void notifyStakeholders(boolean starts){
		
		// si el método es capaz de recibir un argumento del mismo tipo que este se lo pasamos.
		for( List<Object> stakeholderInfo : stakeholders ){
			
			if( starts ){
				
				notifyStakeholder( stakeholderInfo.get(STAKEHOLDER__OBJECT), (String) stakeholderInfo.get(STAKEHOLDER__START) );
			}
			else{
				notifyStakeholder( stakeholderInfo.get(STAKEHOLDER__OBJECT), (String) stakeholderInfo.get(STAKEHOLDER__END) );

			}
		}
	}
	
	/**
	 * 
	 * @param stakeholder
	 * @param methodToCall
	 */
	protected void notifyStakeholder( Object stakeholder, String methodToCall ){
		
		if( methodToCall != null ){
			try {
				stakeholder.getClass().getMethod(methodToCall, this.getClass());
			} catch (NoSuchMethodException | SecurityException e) {
				try {
					stakeholder.getClass().getMethod(methodToCall);
				} catch (NoSuchMethodException | SecurityException e1) {
				}
			}
		}
	}
	
	/**
	 * Establece valores por omisión y ... si algo está mal lanzamos exception???
	 * 
	 * @return
	 */
	public RunnableContractor build(){

		// si el tiempo de espera es null entonces ejecuta sólo 1 vez.
		if( this.waitingFor == null){
			this.shutdown = true;
		}

		return this;
	}
	/**
	 * Despierta al trhead para que ejecute.
	 * 
	 */
	public synchronized RunnableContractor wakeUp(){
		
		this.excecute();
		
		this.notify();
		
		return this;
	}

	public Worker getWorker(){
		return this.worker;
	}
	
}
