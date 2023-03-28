import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Menu;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class VisMain {
	private static final int PACKET_COUNT = 20;
	private static final int SIMULATION_STEPS = 4*2-1; // data send, data recv, ack send, ack recv. two instances of these, but the acks for the last one can be removed, since they are irrelevant for our simulation
	private static final boolean[] DROP_DATA = new boolean[PACKET_COUNT];
	private static final boolean[] DROP_ACK = new boolean[PACKET_COUNT];
	private static final String u1 = "Server";
	private static final String u2 = "Client";
	private static int index  = 0;

	private static JFrame frame1;
	private static JFrame frame2;
	private static Canvas canvas;
	private static JTable tbl;
	private static AbstractTableModel model;

	private static boolean playing = false;
	private static int sim_dist = 0;
	private static double sim_step = 0;
	private static NetStrategy currentStrat = new UnaAckStrat();

	private static final LoadingCache<String, BufferedImage> img = CacheBuilder.newBuilder()
		.build(new CacheLoader<String, BufferedImage>() {
			public BufferedImage load(String key) throws IOException {
				return ImageIO.read(VisMain.class.getClassLoader().getResourceAsStream(key));
			}
		});

	public static String pktName(int packetId) { return "Packet #" + (packetId+1); }

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		frame1 = new JFrame();
		frame1.setTitle("Jugendforscht 2023");
		frame1.setResizable(false);
		frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		canvas = new Canvas();
		canvas.setPreferredSize(new Dimension(800, 600));
		frame1.add(canvas);
		frame1.pack();


		frame2 = new JFrame();
		frame2.setTitle("Jugendforscht 2023");
		frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		model = new AbstractTableModel() {
			public int getColumnCount() {
				return 3+SIMULATION_STEPS;
			}
			public int getRowCount() {
				return PACKET_COUNT;
			}
			public String getColumnName(int col) {
				if(col <= 2) return "";
				return String.valueOf(col - 2);
			}
			public Object getValueAt(int row, int col) {
				if(col == 0) return pktName(row);
				if(col == 1) return DROP_DATA[row];
				if(col == 2) return DROP_ACK[row];
				col -= 3;
				int round = col / 4;
				int step = col % 4;
				if(col >= sim_dist) return "";
				if(step == 0) {
					return doSend(round, row) ? u1 + " sendet " + pktName(row) : "";
				}
				if(step == 1) {
					return doSend(round, row) ? (gotDataPacket(round, row) ? u2 + " empfängt " + pktName(row) : pktName(row) + " geht verloren") : "";
				}
				if(step == 2) {
					return currentStrat.ackFor(round, row) ? u2 + " sendet " + currentStrat.ackName(round, row) : "";
				}
				if(step == 3) {
					return currentStrat.ackFor(round, row) ? (gotAck(round, row) ? u1 + " empfängt " + currentStrat.ackName(round, row) : currentStrat.ackName(round, row) + " geht verloren") : "";
				}
				throw new AssertionError();
			}
			public void setValueAt(Object val, int row, int col) {
				if(col == 1) DROP_DATA[row] = (Boolean) val;
				else if(col == 2) DROP_ACK[row] = (Boolean) val;
				else assert false;
				fireTableDataChanged();
			}

			public Class<?> getColumnClass(int col) {
				return col == 1 || col == 2 ? Boolean.class : String.class;
			}
			public boolean isCellEditable(int row, int col) {
				return col == 1 || col == 2;
			}
		};
		tbl = new JTable(model);
		tbl.setRowSelectionAllowed(false);
		tbl.setColumnSelectionAllowed(false);
		tbl.setCellSelectionEnabled(true);
		tbl.getColumnModel().getColumn(0).setMinWidth(100);
		tbl.getColumnModel().getColumn(0).setMaxWidth(100);
		tbl.getColumnModel().getColumn(1).setMinWidth(16);
		tbl.getColumnModel().getColumn(1).setMaxWidth(16);
		tbl.getColumnModel().getColumn(2).setMinWidth(16);
		tbl.getColumnModel().getColumn(2).setMaxWidth(16);
		canvas.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				System.out.println(e);
				
				switch (e.getKeyCode()) {
					case KeyEvent.VK_1:
						currentStrat = new AckStrat();
						break;
					case KeyEvent.VK_2:
						currentStrat = new UnaStrat();
						break;
					case KeyEvent.VK_3:
						currentStrat = new UnaAckStrat();
						break;
					case KeyEvent.VK_4:
						currentStrat = new BoundaryStrat();
						break;
					case KeyEvent.VK_RIGHT:
						sim_dist ++;
						sim_step = 0;
						break;
					case KeyEvent.VK_LEFT:
						sim_dist --;
						sim_step = 99999;
						break;
					case KeyEvent.VK_ENTER:
						sim_step = 0;
				}

				model.fireTableDataChanged();
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {}
		});
		JScrollPane scroll = new JScrollPane(tbl);
		scroll.setPreferredSize(new Dimension(1000, 350));
		frame2.add(scroll);
		frame2.pack();

		frame2.setVisible(true);
		frame1.setVisible(true);

		while(true) {
			render();
			Thread.sleep(1000/60);
		}
	}

	public static void render() throws ExecutionException {
		BufferStrategy bs = canvas.getBufferStrategy();
		if(bs == null) {
			canvas.createBufferStrategy(2);
			return;
		}
		Graphics2D g = (Graphics2D) bs.getDrawGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		draw(g);
		g.dispose();
		bs.show();
	}

	public static void draw(Graphics2D g) throws ExecutionException {
		// kekse sind lecker
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 800, 600);

		List<Map.Entry<?, ?>> packets = new ArrayList<Map.Entry<?, ?>>();
		if(sim_dist % 4 == 2) {
			packets = Arrays.asList(IntStream.range(0, PACKET_COUNT).filter(a->doSend(sim_dist/4, a)).mapToObj(a->Map.entry(pktName(a), gotDataPacket(sim_dist/4, a))).toArray(Map.Entry<?,?>[]::new));
		}
		if(sim_dist % 4 == 0 && sim_dist > 0) {
			packets = Arrays.asList(IntStream.range(0, PACKET_COUNT).filter(a->currentStrat.ackFor(sim_dist/4-1, a)).mapToObj(a->Map.entry(currentStrat.ackName(sim_dist/4-1, a), gotAck(sim_dist/4-1, a))).toArray(Map.Entry<?,?>[]::new));
		}
		System.out.println(sim_dist);
		g.setColor(Color.black);
		for (int i = 0; i < packets.size(); i++) {
			Map.Entry<?,?> e = packets.get(i);
			double pkt_stp = sim_step*0.6 - (double)i/8;
			if (pkt_stp < 1 && pkt_stp > 0) {
				if (pkt_stp > .5 && !(Boolean) e.getValue()) {
					g.drawImage(img.get("packet.png"), sim_dist % 4 == 2 ? 305 : 320, (int)(80+Math.pow((pkt_stp)*18,2)),50,50, null);
					g.drawString((String) e.getKey(), sim_dist % 4 == 2 ? 305 : 320, (int)(80+Math.pow((pkt_stp)*18,2)));
				} else {
					g.drawImage(img.get("packet.png"), sim_dist % 4 == 2 ? (int)(110+(pkt_stp)*410) :  (int)(500-(pkt_stp)*410), (int)(80+(Math.pow((pkt_stp)*40-20,2)*.5)),50,50, null);
					g.drawString((String) e.getKey(),sim_dist % 4 == 2 ? (int)(110+(pkt_stp)*410) :  (int)(500-(pkt_stp)*410), (int)(80+(Math.pow((pkt_stp)*40-20,2)*.5)));
				}
			}
		}

		g.drawImage(img.get("pc.png"), 100, 250, 100, 100, null);
		g.drawImage(img.get("laptop.png"), 500, 250, 100, 100, null);

		sim_step += .015;
	}

	public static interface NetStrategy {
		public boolean ackFor(int round, int packetId);
		public boolean resendFor(int round, int packetId);
		public String ackName(int round, int packetId);
	}

	public static boolean doSend(int round, int packetId) {
		boolean resend = true;
		for(int i = 0; i < round; i++) {
			if(!currentStrat.resendFor(i, packetId)) resend = false;
		}
		return resend;
	}
	public static boolean gotDataPacket(int round, int packetId) {
		return doSend(round, packetId) && (!DROP_DATA[packetId] || round > 0);
	}
	public static boolean gotAck(int round, int packetId) {
		return currentStrat.ackFor(round, packetId) && (!DROP_ACK[packetId] || round > 0);
	}

	public static class AckStrat implements NetStrategy {
		public String ackName(int round, int packetId) { return "Ack für #" + (packetId+1); }
		public boolean ackFor(int round, int packetId) {
			return gotDataPacket(round, packetId);
		}
		public boolean resendFor(int round, int packetId) {
			return !gotAck(round, packetId);
		}
	}
	public static class UnaStrat implements NetStrategy {
		public String ackName(int round, int packetId) { return "Una bis #" + (packetId+1); }
		public boolean ackFor(int round, int packetId) {
			return gotDataPacket(round, packetId) && (packetId == 0 || ackFor(round, packetId-1) || !doSend(round, packetId-1));
		}
		public boolean resendFor(int round, int packetId) {
			return !gotAck(round, packetId);
		}
	}
	public static class UnaAckStrat implements NetStrategy {
		public String ackName(int round, int packetId) { return ((packetId == 0 || (ackFor(round, packetId-1) && ackName(round, packetId-1).startsWith("Una bis #")) || !doSend(round, packetId-1)) ? "Una bis #" : "Ack für #") + (packetId+1); }
		public boolean ackFor(int round, int packetId) {
			return gotDataPacket(round, packetId);
		}
		public boolean resendFor(int round, int packetId) {
			return !gotAck(round, packetId);
		}
	}
	public static class BoundaryStrat implements NetStrategy {
		public String ackName(int round, int packetId) {
			boolean[] p = new boolean[round == 0 ? packetId + 1 : PACKET_COUNT];
			for(int i = 0; i < p.length; i++) {
				if(!doSend(round, i)) p[i] = true;
				else p[i] = gotDataPacket(round, i) && i <= packetId;
			}
			List<String> chunks = new ArrayList<String>();
			String currChunk = p[0]?"A:1-":"N:1-";
			for(int i = 1; i < p.length; i++) {
				if(currChunk.startsWith(p[i]?"A":"N")) continue;
				
				// end chunk, create new one
				currChunk += i;
				chunks.add(currChunk);
				currChunk = (p[i]?"A:":"N:") + (i+1) + "-";
			}
			currChunk += p.length;
			chunks.add(currChunk);
			return chunks.toString();
		}
		public boolean ackFor(int round, int packetId) {
			return gotDataPacket(round, packetId);
		}
		public boolean resendFor(int round, int packetId) {
			return !gotAck(round, packetId); // TODO
		}
	}
}
