package configgen.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import configgen.RowColumnStream;
import configgen.FlatStream;
import configgen.Main;
import configgen.Utils;
import configgen.data.DataVisitor;
import configgen.data.FStruct;
import configgen.data.Type;

public class Config {
	public final static HashMap<String, Config> configs = new HashMap<String, Config>();
	public final static HashSet<String> refStructs = new HashSet<String>();
	
	private final String name;
	private String type;
	private final String[] files;
	private final String outputDataFile;
	
	private configgen.data.FStruct data;
	public Config(Element data) {
		name = data.getAttribute("name");
		type = name.substring(0, 1).toUpperCase() + name.substring(1) + "Cfg";
		if(configs.put(name, this) != null) {
			throw new RuntimeException("config:" + name + " is duplicate!");
		}
		files = Utils.split(data, "files");
		outputDataFile = Utils.getFileWithoutExtension(files[0]) + ".data";
	}
	
	public String getName() {
		return name;
	}
	
	public final String getType() {
		return type;
	}

	public String[] getFiles() {
		return files;
	}
	
	public final String getOutputDataFile() {
		return outputDataFile;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("config{name=").append(name).append(",type=").append(type);
		sb.append(",files={");
		for(String f : files) {
			sb.append(f).append(",");
		}
		sb.append("}}");
		return sb.toString();
	}
	
	public void verifyDefine() {
		if(name.isEmpty()) {
			throw new RuntimeException("Config name is missing");
		}
		final String t = Alias.getOriginName(type);
		if(t == null || !Field.isStruct(t)) {
			throw new RuntimeException("config:" + name + " type:" + t + "isn't struct!");
		}
		type = t;
	}
	
	public void loadData() throws Exception {
		List<List<String>> lines = new ArrayList<>();
		for(String file : files) {
			file = Main.csvDir + "/" + file;
			System.out.println("load " + name + ", file:" + file);
			lines.addAll(Utils.parse(file));
		}
		
		final FlatStream fs = new RowColumnStream(lines);
		data = new FStruct(null, null, type, fs);
		Main.println(data.toString());
	}
	
	public static void collectRefStructs() {
		configs.values().forEach(c -> collectRef(c.getType()));
		Main.println(refStructs);
	}
	
	static void collectRef(String struct) {
		if(!refStructs.add(struct)) return;
		Struct s = Struct.get(struct);
		for(Field f : s.getFields()) {
			for(String t : f.getTypes()) {
				if(Field.isStruct(t)) {
					collectRef(t);
				}
			}
		}
		for(Struct sub : s.getSubTypes()) {
			collectRef(sub.getName());
		}
		if(!s.getBase().isEmpty()) {
			collectRef(s.getBase());
		}
	}
	
	public static Type getData(String namePath) {
		try {
			final String[] names = namePath.split("\\.");
			Type type = configs.get(names[0]).data;
			for(int i = 1 ; i < names.length ; i++) {
				type = ((FStruct)type).getField(names[i]);
			}
			return type;
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("index data:" + namePath + " can't find");
		}
	}
	
	public void save(Set<String> groups) {
		final DataVisitor vs = new DataVisitor(groups);
		data.accept(vs);
		
		final String outDataFile = Main.dataDir + "/" + this.outputDataFile;
		Utils.save(outDataFile, vs.toData());
		
	}
	
	public void verifyData() {
		data.veryfyData();
	}
	
}
