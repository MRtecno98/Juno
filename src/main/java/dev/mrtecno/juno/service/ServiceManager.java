package dev.mrtecno.juno.service;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class ServiceManager {
	private final Map<Class<? extends Service>, Service> services = new HashMap<>();

	public Optional<Service> register(Service service) {
		return Optional.ofNullable(services.put(service.getClass(), service));
	}

	public Optional<Service> unregister(Class<? extends Service> clazz) {
		return Optional.ofNullable(services.remove(clazz));
	}

	public Optional<Service> get(Class<? extends Service> clazz) {
		return Optional.ofNullable(services.get(clazz));
	}

	public Service require(Class<? extends Service> clazz) {
		return get(clazz).orElseThrow(() -> new IllegalArgumentException("Service not found"));
	}
}
