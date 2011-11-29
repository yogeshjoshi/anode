package org.meshpoint.anode.idl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.meshpoint.anode.type.IValue;

/**
 * A single integer is used to represent a type of any argument
 * or return value of an operation.
 * For interface types, and array<interface> types, the type
 * includes the class id as (TYPE_INTERFACE | (classid << 16))
 * or (TYPE_INTERFACE | TYPE_ARRAY | (classid << 16))
 * @author paddy
 *
 */
public class Types {
	
	/* JS value types */
	public enum JSType {
		UNDEFINED, NULL, BOOLEAN, NUMBER, STRING, OBJECT
	}
	
	public static IValue jsNull;
	public static IValue jsUndefined;
	
	public static class TypeError extends RuntimeException {
		private static final long serialVersionUID = 4694411268187181961L;
	}

	/*
	 * basic types
	 */
	public static final int TYPE_INVALID   = -1;
	public static final int TYPE_NONE      = 0;
	public static final int TYPE_UNDEFINED = 1;
	public static final int TYPE_NULL      = 2;
	public static final int TYPE_BOOL      = 3;
	public static final int TYPE_BYTE      = 4;
	public static final int TYPE_SHORT     = 5;
	public static final int TYPE_INT       = 6;
	public static final int TYPE_LONG      = 7;
	public static final int TYPE_DOUBLE    = 8;
	public static final int TYPE_STRING    = 9;
	public static final int TYPE_MAP       = 10;
	public static final int TYPE_FUNCTION  = 11;
	public static final int TYPE_VALUE     = 12;
	public static final int TYPE_DATE      = 13;

	public static final int TYPE_OBJECT    = 16;
	public static final int TYPE_ARRAY     = 32;
	public static final int TYPE_INTERFACE = 64;
	public static final int OBJECT_MASK    = TYPE_OBJECT-1;
	
	public static boolean isInterface(int type) {
		return (type & TYPE_INTERFACE) != 0;
	}
	
	public static boolean isArray(int type) {
		return (type & TYPE_ARRAY) != 0;
	}
	
	public static short getClassId(int type) {
		return (short)(type >> 16);
	}
	
	public static JSType toJSType(int type) {
		JSType result = JSType.OBJECT;
		if(type < TYPE_OBJECT) {
			switch(type) {
			case TYPE_NONE:
				throw new Types.TypeError();
			case TYPE_UNDEFINED:
				result = JSType.UNDEFINED;
				break;
			case TYPE_NULL:
				result = JSType.NULL;
				break;
			case TYPE_BOOL:
				result = JSType.BOOLEAN;
				break;
			case TYPE_BYTE:
			case TYPE_SHORT:
			case TYPE_INT:
			case TYPE_LONG:
			case TYPE_DOUBLE:
				result = JSType.NUMBER;
				break;
			case TYPE_STRING:
				result = JSType.STRING;
				break;
			default:
			}
		}
		return result;
	}
	
	public static int classid2Type(int classid) { return TYPE_INTERFACE | (classid << 16); }
	public static int type2classid(int type) { return type >> 16; }
	
	public static int fromJavaType(InterfaceManager interfaceManager, Type javaType) {
		/* parameterised and other esoteric types are not supported */
		if(!(javaType instanceof Class))
			return TYPE_INVALID;
		/* handle array types; mutidimensional types are not supported */
		Class<?> javaClass = (Class<?>)javaType;
		if(javaClass.isArray()) {
			Class<?> componentClass = javaClass.getComponentType();
			if(componentClass.isArray()) throw new IllegalArgumentException("Types.fromJavaType: mutidimensional arrays are not supported");
			return TYPE_ARRAY | fromJavaType(interfaceManager, componentClass);
		}
		/* handle basic types */
		int baseClassIndex = classMap.indexOf(javaClass);
		if(baseClassIndex != -1)
			return baseClassIndex;
		/* handle interface types */
		String canonicalName = javaClass.getCanonicalName();
		if(canonicalName.startsWith(INTERFACE_TYPE_PREFIX)) {
			IDLInterface iface = interfaceManager.loadClass(javaClass);
			if(iface != null)
				return classid2Type(iface.getId());
		}
		return TYPE_INVALID;
	}
	
	public static int getInterfaceType(InterfaceManager interfaceManager, IDLInterface obj) {
		int result;
		Class<?> class_ = obj.getClass();
		while(class_ != null) {
			result = fromJavaType(interfaceManager, class_);
			if(result != TYPE_INVALID) return result;
			for(Class<?> interface_ : class_.getInterfaces()) {
				result = fromJavaType(interfaceManager, interface_);
				if(result != TYPE_INVALID) return result;
			}
			class_ = class_.getSuperclass();
		}
		return TYPE_INVALID;
	}
	
	public class Map {}
	public class Function {}
	public class ValueType {}
	public static final String INTERFACE_TYPE_PREFIX = "org.meshpoint.node.iface.";

	/******************
	 * private state
	 ******************/
	private static List<Class<?>> classMap = new ArrayList<Class<?>>(Arrays.asList(new Class<?>[] {
		null,
		Void.TYPE,
		Void.TYPE,
		Boolean.TYPE,
		Byte.TYPE,
		Short.TYPE,
		Integer.TYPE,
		Long.TYPE,
		Double.TYPE,
		String.class,
		Map.class,
		Function.class,
		ValueType.class,
		Date.class
	}));
}