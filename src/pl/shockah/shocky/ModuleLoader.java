package pl.shockah.shocky;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ModuleLoader {
	protected List<Module> modules = Collections.synchronizedList(new ArrayList<Module>());
	
	protected Module loadModule(ModuleSource<?> source) {
		Module m = load(source);
		if (m != null) modules.add(m);
		return m;
	}
	protected void unloadModule(Module module) {
		if (modules.contains(module)) modules.remove(module);
	}
	
	public void unloadAllModules() {
		while (!modules.isEmpty()) Module.unload(modules.get(0));
	}
	
	protected abstract boolean accept(ModuleSource<?> source);
	protected abstract Module load(ModuleSource<?> source);
	
	public static class Java extends ModuleLoader {
		protected boolean accept(ModuleSource<?> source) {
			return source.source instanceof File || source.source instanceof URL;
		}
		protected Module load(ModuleSource<?> source) {
			Module module = null;
			try {
				if (source.source instanceof File) {
					File file = (File)source.source;
					String moduleName = file.getName(); 
					if (moduleName.endsWith(".class")) moduleName = new StringBuilder(moduleName).reverse().delete(0,6).reverse().toString(); else return null;
					if (moduleName.contains("$")) return null;
					
					Class<?> c = new URLClassLoader(new URL[]{file.getParentFile().toURI().toURL()}).loadClass(moduleName);
					if (Module.class.isAssignableFrom(c)) return (Module)c.newInstance();
				} else if (source.source instanceof URL) {
					URL url = (URL)source.source;
					String moduleName = url.toString();
					StringBuilder sb = new StringBuilder(moduleName).reverse();
					moduleName = new StringBuilder(sb.substring(0,sb.indexOf("/"))).reverse().toString();
					String modulePath = new StringBuilder(url.toString()).delete(0,url.toString().length()-moduleName.length()).toString();
					if (moduleName.endsWith(".class")) moduleName = new StringBuilder(moduleName).reverse().delete(0,6).reverse().toString(); else return null;
					if (moduleName.contains("$")) return null;
					
					Class<?> c = new URLClassLoader(new URL[]{new URL(modulePath)}).loadClass(moduleName);
					if (Module.class.isAssignableFrom(c)) return (Module)c.newInstance();
				}
			} catch (Exception e) {e.printStackTrace();}
			return module;
		}
	}
}