package edu.isi.oba.utils.exithandler;

public class DefaultExitHandler implements ExitHandler {
	@Override
	public void exit(int status) {
		System.exit(status);
	}
}
