package one.lindegaard.MobHunting.storage;

public interface IDataCallback_old2<T>
{
	void onCompleted(T data);
	
	void onError(Throwable error);
}
