package me.grax.jbytemod.utils.dialogue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.grax.jbytemod.JByteMod;
import me.lpk.util.OpUtils;

public class InsnEditDialogue extends ClassDialogue {

  private static final HashMap<String, String[]> opc = new LinkedHashMap<>();
  private static final String[] handles;
  private static final String[] frames;

  static {
    opc.put(InsnNode.class.getSimpleName(),
        new String[] { "nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0",
            "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1", "iaload", "laload", "faload", "daload", "aaload", "baload",
            "caload", "saload", "iastore", "lastore", "fastore", "dastore", "aastore", "bastore", "castore", "sastore", "pop", "pop2", "dup",
            "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub", "imul", "lmul",
            "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg", "ishl", "lshl", "ishr",
            "lshr", "iushr", "lushr", "iand", "land", "ior", "lor", "ixor", "lxor", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d",
            "d2i", "d2l", "d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ireturn", "lreturn", "freturn", "dreturn",
            "areturn", "return", "arraylength", "athrow", "monitorenter", "monitorexit" });
    opc.put(MethodInsnNode.class.getSimpleName(), new String[] { "invokestatic", "invokevirtual", "invokespecial", "invokeinterface" });
    opc.put(FieldInsnNode.class.getSimpleName(), new String[] { "getstatic", "putstatic", "getfield", "putfield" });
    opc.put(VarInsnNode.class.getSimpleName(),
        new String[] { "iload", "lload", "fload", "dload", "aload", "istore", "lstore", "fstore", "dstore", "astore", "ret" });
    opc.put(TypeInsnNode.class.getSimpleName(), new String[] { "new", "anewarray", "checkcast", "instanceof" });
    opc.put(LdcInsnNode.class.getSimpleName(), new String[] { "ldc" });
    opc.put(IincInsnNode.class.getSimpleName(), new String[] { "iinc" });
    opc.put(JumpInsnNode.class.getSimpleName(), new String[] { "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt",
        "if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ifnull", "ifnonnull" });
    opc.put(IntInsnNode.class.getSimpleName(), new String[] { "bipush", "sipush", "newarray" });
    opc.put(InvokeDynamicInsnNode.class.getSimpleName(), new String[] { "invokedynamic" });
    opc.put(TableSwitchInsnNode.class.getSimpleName(), new String[] { "tableswitch" });
    opc.put(LookupSwitchInsnNode.class.getSimpleName(), new String[] { "lookupswitch" });
    opc.put(LabelNode.class.getSimpleName(), null);
    opc.put(LineNumberNode.class.getSimpleName(), null);
    opc.put(FrameNode.class.getSimpleName(), null);
    handles = new String[] { "h_getfield", "h_getstatic", "h_putfield", "h_putstatic", "h_invokevirtual", "h_invokestatic", "h_invokespecial",
        "h_newinvokespecial", "h_invokeinterface" };
    frames = new String[] { "f_new", "f_full", "f_append", "f_chop", "f_same", "f_same1" };
  }

  private MethodNode mn;

  public InsnEditDialogue(MethodNode mn, Object object) {
    super(object);
    this.mn = mn;
  }

  @Override
  public boolean open() {
    Object object = getObject();
    if (object instanceof LdcInsnNode) {
      //special case for LdcInsnNode
      LdcInsnNode ldc = (LdcInsnNode) object;
      JPanel mainPanel = new JPanel();
      JPanel leftText = new JPanel();
      JPanel rightInput = new JPanel();

      mainPanel.setLayout(new BorderLayout());
      leftText.setLayout(new GridLayout(0, 1));
      rightInput.setLayout(new GridLayout(0, 1));

      leftText.add(new JLabel("Ldc Type: "));
      JComboBox<String> ldctype = new JComboBox<String>(new String[] { "String", "float", "double", "int", "long" });
      if (ldc.cst instanceof String) {
        ldctype.setSelectedItem("String");
      } else if (ldc.cst instanceof Float) {
        ldctype.setSelectedItem("float");
      } else if (ldc.cst instanceof Double) {
        ldctype.setSelectedItem("double");
      } else if (ldc.cst instanceof Long) {
        ldctype.setSelectedItem("long");
      } else if (ldc.cst instanceof Integer) {
        ldctype.setSelectedItem("int");
      } else {
        throw new RuntimeException("Unsupported LDC Type: " + ldc.cst.getClass().getName());
      }
      rightInput.add(ldctype);
      leftText.add(new JLabel("Ldc Value: "));
      JTextField cst = new JTextField(ldc.cst.toString());
      rightInput.add(cst);

      mainPanel.add(leftText, BorderLayout.WEST);
      mainPanel.add(rightInput, BorderLayout.CENTER);

      if (JOptionPane.showConfirmDialog(null, mainPanel, "Edit LdcInsnNode", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
          switch (ldctype.getSelectedItem().toString()) {
          case "String":
            ldc.cst = cst.getText();
            break;
          case "float":
            ldc.cst = Float.parseFloat(cst.getText());
            break;
          case "double":
            ldc.cst = Double.parseDouble(cst.getText());
            break;
          case "long":
            ldc.cst = Long.parseLong(cst.getText());
            break;
          case "int":
            ldc.cst = Integer.parseInt(cst.getText());
            break;
          }
        } catch (Exception e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(null, "Exception: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          return false;
        }
        return true;
      }
      return false;
    }

    return super.open();
  }

  public static void createInsertInsnDialog(MethodNode mn, AbstractInsnNode ain, boolean after) {
    final JPanel panel = new JPanel(new BorderLayout(5, 5));
    final JPanel input = new JPanel(new GridLayout(0, 1));
    final JPanel labels = new JPanel(new GridLayout(0, 1));
    panel.add(labels, "West");
    panel.add(input, "Center");
    labels.add(new JLabel("Type"));
    JComboBox<String> clazz = new JComboBox<String>(new ArrayList<String>(opc.keySet()).toArray(new String[0]));
    input.add(clazz);
    if (JOptionPane.showConfirmDialog(JByteMod.instance, panel, "Insert " + (after ? "after" : "before"), 2) == JOptionPane.OK_OPTION) {
      try {
        //only works because i created constructors for those nodes
        Class<?> node = Class.forName("org.objectweb.asm.tree" + "." + clazz.getSelectedItem().toString());
        AbstractInsnNode newnode = (AbstractInsnNode) node.getConstructor().newInstance();
        //TODO initialize some values
        //we need no edit for LabelNode
        if (!hasSettings(newnode) || new InsnEditDialogue(mn, newnode).open()) {
          if (ain != null) {
            if (after) {
              mn.instructions.insert(ain, newnode);
            } else {
              mn.instructions.insertBefore(ain, newnode);
            }
          } else {
            mn.instructions.add(newnode);
          }
          JByteMod.instance.getCodeList().loadInstructions(mn);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }

  @Override
  protected boolean ignore(String name) {
    return name.equals("itf");
  }

  @Override
  protected void addSpecial(Object obj, JPanel leftText, JPanel rightInput) {
    if (obj instanceof AbstractInsnNode) {
      AbstractInsnNode ain = (AbstractInsnNode) obj;
      String[] arr = opc.get(ain.getClass().getSimpleName());
      if (arr == null) {
        return;
      }
      leftText.add(new JLabel("Opcode: "));
      JComboBox<String> opcode = new JComboBox<String>(arr);
      opcode.setSelectedItem(OpUtils.getOpcodeText(ain.getOpcode()).toLowerCase());
      rightInput.add(wrap("opc", opcode));
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Object getSpecialValue(Object object, String name, Class<?> type, Object o, WrappedPanel wp) {
    System.out.println(name + " " + o);
    if (o != null && o.equals("opc")) {
      JComboBox<String> opcode = (JComboBox<String>) wp.getComponent(0);
      AbstractInsnNode ain = (AbstractInsnNode) object;
      ain.setOpcode(OpUtils.getOpcodeIndex(String.valueOf(opcode.getSelectedItem()).toUpperCase()));
      return null;
    } else if (type.getName().equals(LabelNode.class.getName())) {
      JComboBox<LabelNode> label = (JComboBox<LabelNode>) wp.getComponent(0);
      return label.getSelectedItem();
    } else if (name.equals("tag") && type.getName().equals(int.class.getName())) {
      JComboBox<String> label = (JComboBox<String>) wp.getComponent(0);
      return label.getSelectedIndex() + 1;
    } else if (name.equals("type") && type.getName().equals(int.class.getName())) {
      System.out.println(o);
      JComboBox<String> label = (JComboBox<String>) wp.getComponent(0);
      return label.getSelectedIndex() - 1;
    }
    return null;
  }

  @Override
  protected boolean isSpecial(String name, Class<?> type) {
    return type.getName().equals(LabelNode.class.getName()) 
        || (name.equals("tag") && type.getName().equals(int.class.getName()))  //invokedynamic tag
        || (name.equals("type") && type.getName().equals(int.class.getName())); //frame type
  }

  @Override
  protected Component getSpecial(Object o, String name, Class<?> type) {
    if (type.getName().equals(LabelNode.class.getName())) {
      ArrayList<LabelNode> ln = new ArrayList<>();
      for (AbstractInsnNode nod : mn.instructions.toArray()) {
        if (nod instanceof LabelNode) {
          ln.add((LabelNode) nod);
        }
      }
      JComboBox<LabelNode> jcb = new JComboBox<>(ln.toArray(new LabelNode[0]));
      jcb.setSelectedItem(o);
      return jcb;
    } else if (name.equals("tag")) {
      JComboBox<String> jcb = new JComboBox<>(handles);
      jcb.setSelectedIndex(((int) o) - 1);
      return jcb;
    } else if (name.equals("type")) {
      JComboBox<String> jcb = new JComboBox<>(frames);
      jcb.setSelectedIndex(((int) o) + 1);
      return jcb;
    }
    return null;
  }

  public static boolean canEdit(AbstractInsnNode ain) {
    String sn = ain.getClass().getSimpleName();
    return opc.keySet().contains(sn) && hasSettings(ain);
  }

  private static boolean hasSettings(AbstractInsnNode ain) {
    return !(ain instanceof LabelNode);
  }

  @Override
  protected ClassDialogue init(Object value) {
    return new InsnEditDialogue(mn, value);
  }
}
