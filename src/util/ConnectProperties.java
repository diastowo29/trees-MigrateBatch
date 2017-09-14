package util;

public class ConnectProperties {
	public Boolean isTesting = true;

	public enum methods {
		GET, POST, PUT, DELETE
	}

	public enum api {
		userCreate, userUpdate, userDelete
	}
}