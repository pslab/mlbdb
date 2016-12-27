/*
 * File    : DiskManagerImpl.java
 * Created : 18-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.core3.disk;
 
 
import java.io.File;

import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.IndentWriter;

/**
* @author MjrTom
*			2005/Oct/08: Priority, getPieces done, etc changes for new piece-picking
*/

public interface
DiskManager
{
	public static final int INITIALIZING = 1;
	public static final int ALLOCATING = 2;
	public static final int CHECKING = 3;
	public static final int READY = 4;
	public static final int FAULTY = 10;
	
	
		// CHANGE THIS AND YOU MUST CHANGE NORMAL_REQUEST_SIZE in PeerReadRequest (plugin interface)
	
	public static final int BLOCK_SIZE_KB 	= 16;
	public static final int BLOCK_SIZE 		= BLOCK_SIZE_KB*1024;

	/**
	 * Start checking/allocating
	 */
	public void
	start();
	
	public void
	stop(
		boolean	closing );
	
	/**
	  * @return whether all files exist and sizes match
	  */
	
	public boolean
	filesExist();

	public DirectByteBuffer 
	readBlock(
		int pieceNumber, 
		int offset, 
		int length );
	
	public DiskManagerWriteRequest
	createWriteRequest(
		int 				pieceNumber,
		int 				offset,
		DirectByteBuffer 	data,
		Object 				user_data );

	
		/**
		 * enqueue an async write request
		 * @param pieceNumber
		 * @param offset
		 * @param data
		 * @param user_data	this will be provided to the listener when called back
		 * @param listener
		 */
	
	public void 
	enqueueWriteRequest(
		DiskManagerWriteRequest			request,
		DiskManagerWriteRequestListener	listener );

	public boolean
	hasOutstandingWriteRequestForPiece(
		int		piece_number );
	
	public DiskManagerReadRequest
	createReadRequest(
		int pieceNumber,
		int offset,
		int length );
	
		  /**
		   * Enqueue an async disk read request.
		   * @param request
		   * @param listener
		   */
	
	public void 
	enqueueReadRequest( 
		DiskManagerReadRequest 			request, 
		DiskManagerReadRequestListener 	listener );

		/**
		 * Create a request to check a particular piece
		 * @param pieceNumber	-1 for a complete recheck request
		 * @param user_data
		 * @return
		 */
	
	public DiskManagerCheckRequest
	createCheckRequest(
		int 		pieceNumber,
		Object		user_data );
	
		/**
		 * enqueue an asynchronous single piece check
		 * @param pieceNumber
		 * @param listener
		 * @param user_data
		 */
	
	public void
	enqueueCheckRequest(
		DiskManagerCheckRequest			request,
		DiskManagerCheckRequestListener	listener );
	
		/**
		 * recheck the entire torrent asynchronously, reporting each piece to the listener
		 * @param listener
		 * @param user_data
		 */
	
	public void
	enqueueCompleteRecheckRequest(
		DiskManagerCheckRequest			request,
		DiskManagerCheckRequestListener	listener );
	
	public void
	setPieceCheckingEnabled(
		boolean		enabled );
			
	public void
    saveResumeData(
    	boolean interim_save )
		
		throws Exception;

	
	public DiskManagerPiece[] 
	getPieces();
	
	public int 
	getNbPieces();

	public DiskManagerFileInfo[] getFiles();
	public DiskManagerPiece getPiece(int PieceNumber);

	public DMPieceList getPieceList(int pieceNumber);
	
	public int
	getState();
	
	public long
	getTotalLength();
	
	public int
	getPieceLength();
	
	public int
	getPieceLength(
		int	piece_number );
	
	public long
	getRemaining();
	
	public long
	getRemainingExcludingDND();
	
	public int
	getPercentDone();
	
	public String
	getErrorMessage();
  
	public void
	downloadEnded();

    public void
    downloadRemoved();
	
	public void moveDataFiles(File new_parent_dir, String dl_name);
	
		/**
		 * returns -1 if no recheck in progress, percentage complete in 1000 notation otherwise
		 * @return
		 */
	
	public int 
	getCompleteRecheckStatus();
  
		/**
		 * method for checking that the block details are sensible
		 * @param pieceNumber
		 * @param offset
		 * @param data
		 * @return
		 */
	
	public boolean 
	checkBlockConsistencyForWrite(
		String				originator,
		int 				pieceNumber, 
		int 				offset, 
		DirectByteBuffer 	data );

		/**
		 * method for checking that the block details are sensible
		 * @param pieceNumber
		 * @param offset
		 * @param length
		 * @return
		 */
	
	public boolean 
	checkBlockConsistencyForRead(
		String	originator,
		int 	pieceNumber, 
		int	 	offset, 
		int 	length );
		
	public boolean 
	checkBlockConsistencyForHint(
		String	originator,
		int 	pieceNumber, 
		int	 	offset, 
		int 	length );
	
	public TOTorrent
	getTorrent();
	
	public File
	getSaveLocation();
	
	public void
	addListener(
		DiskManagerListener	l );
	
	public void
	removeListener(
		DiskManagerListener	l );
  
  
  /**
   * Save the individual file priorities map to
   * DownloadManager.getData( "file_priorities" ).
   */

	public void 
	saveState();

	/**
	 * @param pieceNumber
	 * @return true if the pieceNumber is Needed and not Done
	 */
	public boolean isInteresting(int pieceNumber);
	
	public boolean isDone(int pieceNumber);

	public int getCacheMode();
	
	public void
	generateEvidence(
		IndentWriter		writer );
}