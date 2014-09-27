package mekanism.common;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

/**
 * Internal interface used for blocks that send data between clients and the server
 * @author AidanBrady
 *
 */
public interface ITileNetwork
{
	/**
	 * Receive and manage a packet's data.
	 * @param network
	 * @param packet
	 * @param player
	 * @param dataStream
	 */
	public void handlePacketData(ByteBuf dataStream) throws Exception;

	/**
	 * Gets an ArrayList of data this tile entity keeps synchronized with the client.
	 * @param data - list of data
	 * @return ArrayList
	 */
	public ArrayList getNetworkedData(ArrayList data);
}
