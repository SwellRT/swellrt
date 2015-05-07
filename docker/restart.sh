PID=`ps -eo pid,cmd | grep 'java.*\-jar.*wave.*jar' | grep -v 'grep' | sed  's/^\ *//' | cut -d\  -f 1`

if [ -n "$PID" ]
then
  kill $PID
  sleep 1
fi

DIR=`dirname $0`
exec "${DIR}/server.sh"
