package dev.mrtecno.juno.service;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@SuppressWarnings("unchecked")
public class ServiceManager {
	private final Map<Class<? extends Service>, Service> services = new HashMap<>();

	public <T extends Service> Optional<T> register(Class<T> clazz, T service) {
		return Optional.ofNullable((T) services.put(clazz, service));
	}

	public <T extends Service> Optional<T> register(T service) {
		return register((Class<T>) service.getClass(), service);
	}

	public <T extends Service> Optional<T> unregister(Class<T> clazz) {
		return Optional.ofNullable((T) services.remove(clazz));
	}

	public <T extends Service> Optional<T> get(Class<T> clazz) {
		return Optional.ofNullable((T) services.get(clazz));
	}

	public Service require(Class<? extends Service> clazz) {
		return get(clazz).orElseThrow(() -> new IllegalArgumentException("Service not found"));
	}
}
