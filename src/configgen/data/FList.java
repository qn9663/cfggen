package configgen.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import configgen.FlatStream;
import configgen.Main;
import configgen.RowColumnStream;
import configgen.Utils;
import configgen.type.Config;
import configgen.type.Field;

public class FList extends Type {
	public final List<Type> values = new ArrayList<Type>();
	public final HashMap<String, HashSet<Type>> indexs = new HashMap<String, HashSet<Type>>();

	public FList(FStruct host, Field define, FlatStream is) {
		super(host, define);
		Field valueDefine = define.stripAdoreType();
		while(!is.isSectionEnd()) {
			final Type value = Type.create(host, valueDefine, is);
			values.add(value);
			if(host == null) {
				Main.addLastLoadData(value);
			}
		}
		
		for(String idx : define.getIndexs()) {
			final HashSet<Type> m = new HashSet<Type>();
			for(Type v : values) {
				FStruct s = (FStruct)v;
				Type key = s.getField(idx);
				if(!m.add(key)) 
					throw new RuntimeException(String.format("field:%s idx:%s key:%s duplicate!", define, idx, key));
			}
			indexs.put(idx, m);
		}
	}
	
	public FList(FStruct host, Field define, Element ele) {
		super(host, define);
		Field valueDefine = define.stripAdoreType();
		final NodeList nodes = ele.getChildNodes();
		for(int i = 0, n = nodes.getLength() ; i < n ; i++) {
			final Node node = nodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				final Type value = Type.create(host, valueDefine, (Element)node);
				values.add(value);
				if(host == null) {
					Main.addLastLoadData(value);
				}
			}
		}
		
		for(String idx : define.getIndexs()) {
			final HashSet<Type> m = new HashSet<Type>();
			for(Type v : values) {
				FStruct s = (FStruct)v;
				Type key = s.getField(idx);
				if(!m.add(key)) 
					throw new RuntimeException(String.format("field:%s idx:%s key:%s duplicate!", define, idx, key));
			}
			indexs.put(idx, m);
		}
	}

	public FList(FStruct host, Field define, File f) {
		super(host, define);
		Field valueDefine = define.stripAdoreType();
		
		for(File file : f.listFiles()) {
			//Main.println("====== load:" + file.getAbsolutePath());
			try {
				final Type value = file.getName().endsWith(".xml") ?
					 Type.create(host, valueDefine, DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement())
					:Type.create(host, valueDefine, new RowColumnStream(Utils.parse(file.getAbsolutePath())));
				
				values.add(value);
				// 说明是配置行数据,加入最近读取的数据列表
				if(host == null) {
					Main.addLastLoadData(value);
				}
			} catch (Exception e) {
				System.out.printf("file:%s load fail\n", file.getAbsolutePath());
				throw new RuntimeException(e);
			}
		}
		
		for(String idx : define.getIndexs()) {
			final HashSet<Type> m = new HashSet<Type>();
			for(Type v : values) {
				FStruct s = (FStruct)v;
				Type key = s.getField(idx);
				if(!m.add(key)) 
					throw new RuntimeException(String.format("field:%s idx:%s key:%s duplicate!", define, idx, key));
			}
			indexs.put(idx, m);
		}
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("List<").append(define.getFullType()).append(">{");
		values.forEach(v -> sb.append(v).append(","));
		sb.append("}");
		return sb.toString();
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public void verifyData() {
		final String ref = define.getRef();
		if(!ref.isEmpty()) {
			HashSet<Type> validValues = Config.getData(ref);
			for(Type d : values) {
				if(!validValues.contains(d))
					errorRef(d);
			}
		}
		if(Field.isStruct(define.getTypes().get(1)))
			for(Type d : values) {
				d.verifyData();
			}
	}
	
}
