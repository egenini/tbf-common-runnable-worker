package ar.com.tbf.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.tbf.runnable.pool.RunnableWorkerPool;

public class PooleableRunnableWorker extends RunnableWorker {

	private Logger  log        = LoggerFactory.getLogger(PooleableRunnableWorker.class);
	private boolean invalid    = false;
	private String  poolKey    = "";
	// pool times
	private long onActivateTime  = 0;
	private long onPassivateTime = 0;
	private long onValidateTime  = 0;

	public PooleableRunnableWorker(Worker worker) {
		super(worker);
		
		this.pooleable = true;
		this.setPoolKey(worker.getClass().getName());
	}

	public PooleableRunnableWorker( Worker worker, long timeToWait ){

		super(worker, timeToWait);

		this.pooleable = true;
		this.setPoolKey(worker.getClass().getName());
	}
	
	protected void returnToPool() {
		
		try {

			RunnableWorkerPool.getInstance().getPool().returnObject( this.getPoolKey(), this );

			//System.out.println("Volvió al pool "+ this.getId() +" es válido "+ ! this.invalid );
			
		} catch (Exception e) {
		
			e.printStackTrace();
			
			this.invalid = true;
			log.error( "error retornando al poll con clave "+ this.getPoolKey() +" id "+ this.getId(), e );
		}

	}
	public void onPassivate() {
		this.onPassivateTime = System.currentTimeMillis();
		//this.onActivateTime  = 0;
		//this.onValidateTime  = 0;
	}

	public boolean onValidate() {

		this.onValidateTime = System.currentTimeMillis();
		
		if( ! this.invalid ) {
			
			if( this.worker != null ) {
				
				this.invalid = ! this.worker.canWork();
			}
		}

		//System.out.println("onValidate "+ this.getId() +" "+ ! this.invalid );
		
		return ! this.invalid;
	}

	public void onActivate() {
		this.onActivateTime  = System.currentTimeMillis();
		this.onPassivateTime = 0;
		this.posWorkTime     = 0;
	}

	public void setInvalidate() {
		
		System.out.println("Invalidando id "+ this.getId() );
		this.invalid = true;
	}

	public void onDestroy() {
		
		//System.out.println("onDestroy "+ this.getId() );
		
		if( this.worker != null ) {
			
			this.worker.releaseResources();
			
		}

		if( ! this.isShutdown() ) {
			
			System.out.println("Bajando thread "+ this.getId() );

			this.setShutdown(true);
		}
	}

	public PooleableRunnableWorker withId( int id ) {
		
		super.setId(id);
		
		return this;
	}
	public String printPoolStatus() {
		
		return "{ \"onActivateTime\": "+ this.onActivateTime +", \"onPassivateTime\":"+ this.onPassivateTime +", \"onValidateTime\": "+ this.onValidateTime +"}"; 
	}
	
	public String printStatus() {
		
		return "{ \"workStatus\": "+ super.printStatus() +", \"poolStatus\": "+ printPoolStatus();
	}

	public String getPoolKey() {
		return poolKey;
	}

	public void setPoolKey(String poolKey) {
		this.poolKey = poolKey;
	}

	public long getOnActivateTime() {
		return onActivateTime;
	}

	public void setOnActivateTime(long onActivateTime) {
		this.onActivateTime = onActivateTime;
	}

	public long getOnPassivateTime() {
		return onPassivateTime;
	}

	public void setOnPassivateTime(long onPassivateTime) {
		this.onPassivateTime = onPassivateTime;
	}

	public long getOnValidateTime() {
		return onValidateTime;
	}

	public void setOnValidateTime(long onValidateTime) {
		this.onValidateTime = onValidateTime;
	}

}
