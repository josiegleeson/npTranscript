# this is run in the results director from npTranscript
todo=$(ls *.clusters.zip | xargs -I {} echo {} | rev | cut -f 3- -d . | rev)
npTranscript=${HOME}/github/npTranscript
for i in $todo; do
	less $i.clusters.zip | grep '.fa' > $i.todo.txt
	#cnt=$(wc -l $i.todo.txt | cut -f 1 -d ' ')
	cnt=$(less $i.clusters.zip | grep '.fa'  | wc -l)
  	echo $i $cnt
  	ls $i.clusters.zip
 	sbatch --array 1-${cnt} ${npTranscript}/scripts/consensus.sh $i .fa
done
