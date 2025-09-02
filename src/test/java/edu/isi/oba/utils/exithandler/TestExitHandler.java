package edu.isi.oba.utils.exithandler;

public class TestExitHandler implements ExitHandler {
	public int status = -1;

	@Override
	public void exit(int status) {
		this.status = status;
		throw new RuntimeException("System.exit called with status: " + status);
	}
}
