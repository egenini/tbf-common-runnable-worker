package ar.com.tbf.runnable;

public interface Worker {

	public void work();
	public boolean canWork();
	public void releaseResources();
}
