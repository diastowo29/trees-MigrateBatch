package util;

public class ConnectProperties {
	public Boolean isDeploying = true;

	public enum methods {
		GET, POST, PUT, DELETE
	}

	public enum api {
		userCreate, userUpdate, userDelete
	}
}