package util;

public class ConnectProperties {
	public Boolean isTesting = false;

	public enum methods {
		GET, POST, PUT, DELETE
	}

	public enum api {
		userCreate, userUpdate, userDelete
	}
}