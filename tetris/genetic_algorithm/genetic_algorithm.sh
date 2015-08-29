#!/bin/bash
# try genetic_algorithm.sh --help

#**************************************
#		Algorithm
#**************************************

# Soit S un ensemble de sets de paramètres, et i son indice
# Soit B un ensemble des meilleurs sets selon leur performance, et i son indice
#
# i=0
# Si := init
#
# loop
# 	run ( Si )
# 	Bi := survive ( Si )
# 	Si+1 := pairwisemixing ( Si )
# 	Si+1 := mutation ( Si+1 )
# 	Si+1 += Bi
# 	i++
# end loop
#
# init crée un ensemble de sets de paramètres aléatoires
# run lance le programme concerné avec chaque set de paramètres, séquentiellement
# survival renvoie les meilleurs sets
# pairwisemixing crée un nouvel ensemble de sets
# 	en mélangeant les sets 2 à 2, en faisant la moyenne de chaque paramètre
# mutation altère les paramètres des sets nouvellement créés, de manière aléatoire
#
# Par analogie avec le vivant, on appelera init "create_life",
# run se transformera en "live",
# survival et mutation changeront peu en "survive" et "mutate",
# la partie pairwise de pairwisemixing deviendra "couple",
# et la partie mixing sera nommée "give_life".
# Enfin, live, survive, couple, give_life et mutate seront agréguées dans
# ce qu'on appelera "evolution".


#**************************************
#		Global Variables
#**************************************

# the following variables will be filled while argument analysis
# it means, whenever you fill them now, they may be overwritten
# if not, these values will be used as defaults.
# just remember to FILL THEM, here or on the command line, 
# because errors may occur if they're empty
opt_skipinit=0
opt_skipevolve=0
opt_skiplive=0
opt_show_unique=0
opt_show_reverse=0
generation=0
ign_pre=0
ign_exec=0
ign_post=0
survive='50%'
fittest_is='h'
cycle=0 # infinite
opt_plot=
# directory to output sets
set_dir_root="${0%/*}/sets"
# set results directory
result_dir_root="${0%/*}/results"
# surviving sets directory
survive_dir="${0%/*}/survivors"
# children directory
children_dir="${0%/*}/children"

# the following variables WILL BE OVERWRITTEN IN MAIN
last_set=
random=
average=
alterate=

# control variables, MUST NOT BE CHANGED
opt_param=0
opt_pref=0
opt_postf=0
opt_resf=0
opt_mutate=0
opt_error=0


#**************************************
#		Functions
#**************************************

# show usage
usage() {
	local col=$(/usr/bin/tput cols)
	echo "usage: ${0##*/} -h,--help"
	echo
	echo "${0##*/} -E --init-set INT>0 -t int|float -S INT>0 <-m | --minvalue INT --maxvalue INT>" | /usr/bin/fmt -w $col
	echo
	echo "${0##*/} -I [-L] [-c INT>=0] -d DIR -e CMD -r FILE [-f h|l] [-g INT>=0] [-i 1..7 ...] --minmutate INT --maxmutate INT -p FILE [--pre-f FILE] [--post-f FILE] --result-f FILE -s INT>=0 -t int|float [-S INT]" | /usr/bin/fmt -w $col
	echo
	echo "Without both -E and -I: combination of the two previous usages" | /usr/bin/fmt -w $col
	echo "Additional: --children-dir DIR --set-dir DIR --result-dir DIR --survive-dir DIR" | /usr/bin/fmt -w $col
	echo "Options in brackets [] have default values (except scale witch is required if type = float), or are not needed"
	echo
	echo "Specific usage for showing results: ${0##*/} --show [-d DIR] [ -g set|gen | [-s set|gen] [-u] [-r] ]" | /usr/bin/fmt -w $col
	echo "Option -g (--graph) needs gnuplot to be installed" | /usr/bin/fmt -w $col
	exit 1
}

write_bold() {
	echo -e "\033[1m$*\033[0m"
}

# show help
help() {
	local col=$(/usr/bin/tput cols)
	write_bold "Synopsis"
	echo "    ${0##*/} - uses the genetic algorithm to generate efficient sets of parameters" | /usr/bin/fmt -w "$col"
	echo
	write_bold "Description"
	echo "    https://en.wikipedia.org/wiki/Genetic_algorithm"
	echo
	write_bold "Options"
	echo "    Grâce à ses options, ce script est plutôt générique. \
Il est possible de définir la commande à lancer (par exemple une intelligence \
artificielle basée sur des paramètres qu'on voudrait améliorer), des fonctions \
de pre-exécution et de post-exécution, ainsi qu'une fonction de récupération \
des résultats. Il faudra donc également indiquer le dossier d'exécution de la commande, \
ainsi que le fichier dans lequel récupérer les résultats. \
Actuellement, la comparaison des performances ne fonctionne \
qu'avec des résultats mono-valués. Il est également (et heureusement) possible \
de définir les noms des paramètres qui seront utilisés. Votre programme doit \
utiliser ces paramètres sous la forme d'un fichier texte :" | /usr/bin/fmt -w $col
	echo "        parametre1=valeur1"
	echo "        parametre2=valeur2"
	echo "        ..."
	echo "        parametreN=valeurN"
	echo
	echo "    Vous pourrez donc indiquer un fichier dans lequel les noms des paramètres \
seront donnés, sous cette forme :" | /usr/bin/fmt -w $col
	echo "        param[0]=nom_parametre0"
	echo "        param[1]=nom_parametre1"
	echo "        ..."
	echo "        param[N]=nom_parametreN"
	echo
	echo "    En effet le script utilise un tableau de paramètre nommé 'param'. \
Le nombre de paramètres n'est pas un problème, le script saura le déterminer. \
Les autres options sur la ligne de commandes permettent de définir des intervalles \
(intervalle de valeurs pour la création de la première génération, intervalle de valeurs \
pour la mutation des sets de paramètres des générations suivantes), un type de paramètre \
(entier ou flottant), ainsi qu'une précision dans le cas du type flottant, \
le numéro de génération avec laquelle commencer, un nombre constant \
ou un pourcentage des individus qui survivront à chaque génération, \
le nombre de cycle d'évolution à itérer (0 pour l'infini), \
et enfin si les survivants seront ceux ayant obtenu les scores (ou performance) \
les plus ELEVES ou les plus FAIBLES (dépendemment du programme testé)." | /usr/bin/fmt -w $col
	echo "    Le script gère également les erreurs d'exécution, mais vous pourrez \
choisir de les ignorer (exécution, pré-exécution, post-exécution). \
Vous pouvez encore choisir de ne pas procéder à la première étape (création de \
la première génération), ou à la deuxième étape (évolution). Si vous annulez les deux, \
il ne se passera évidemment RIEN. Pour finir, vous pouvez aussi définir les dossiers \
à utiliser pour le stockage des sets de paramètres, des résultats correspondant, et les \
dossiers accueillants les survivants et enfants temporaires." | /usr/bin/fmt -w $col
	echo
	write_bold "    Help & Usage"
	echo
	echo "        (no argument)"
	echo "                Show usage of the script and exit (1)"
	echo "        -h, --help"
	echo "            Show this help and exit (0)"
	echo
	write_bold "    Showing results"
	echo
	echo "        --show"
	echo "            Show all results, numerically sorted (from lowest to highest by default), \
with set index and generation" | /usr/bin/fmt -w $col
	echo "        -d, --directory DIR"
	echo "            Directory in which to search for results. \
Default is $result_dir_root" | /usr/bin/fmt -w $col
	echo "        -u, --unique"
	echo "            Show only highest or lowest score for each set" | /usr/bin/fmt -w $col
	echo "        -r, --reverse"
	echo "            Show all results sorted from highest to lowest" | /usr/bin/fmt -w $col
	echo "        -s, --sum set|gen[eration]"
	echo "            Show sorted sums by set or by generation. Option --unique is useless here" | /usr/bin/fmt -w $col
	echo "        -g, --graph set|gen[eration]"
	echo "            Outputs a graph showing evolution of the sets or the generations in a PNG file. Overpass any other option except -d" | /usr/bin/fmt -w $col
	echo
	write_bold "    Initial sets"
	echo
	echo "        --init-set N"
	echo "            Defines how many sets have to be created while initialization. \
N has to be positive and not null" | /usr/bin/fmt -w $col
	echo "        -m, --mutate-from BASE"
	echo "            The created sets will be mutations of the BASE set, not sets of random values" | /usr/bin/fmt -w $col
	echo "        --minvalue V"
	echo "            Defines the minimum value for the interval of random values for each \
parameter in a set. This value is not needed if option --mutate-from is given" | /usr/bin/fmt -w $col
	echo "        --maxvalue V"
	echo "            Defines the maximum value for the interval of random values for each \
parameter in a set. This value is not needed if option --mutate-from is given" | /usr/bin/fmt -w $col
	echo "        -I, --skip-init"
	echo "            Disables initialization. It means that no sets are randomly created \
for generation 0. However, option --mutate-from overpass this option" | /usr/bin/fmt -w $col
	echo
	write_bold "    Parameters"
	echo
	echo "        -t, --type TYPE"
	echo "            Defines parameter type. TYPE is one of 'int', 'integer', or 'float'" | /usr/bin/fmt -w $col
	echo "        -S, --scale S"
	echo "            If type is float, you need to specify a scale. Must be greater or equal to zero" | /usr/bin/fmt -w $col
	echo "        -p, --parameters FILE"
	echo "            FILE contains parameter names to use. It is written like this:" | /usr/bin/fmt -w $col
	echo "            param[0]=name0"
	echo "            param[1]=name1"
	echo "            ..."
	echo "            param[N]=nameN"
	echo
	write_bold "    Evolution"
	echo
	echo "        -c, --cycle C"
	echo "            Number of cycles of evolution. C=0 means infinite evolution. Default is $cycle" | /usr/bin/fmt -w $col
	echo "        -f, --fittest lowest|l|highest|h"
	echo "            Those which survive are the lowest / the highest. Default is $fittest_is" | /usr/bin/fmt -w $col
	echo "        -g, --generation G"
	echo "            Specify at which generation to begin. You can use this option to \
resume a previous evolution. With G>0, initialization (with or without --mutate-from) \
is skipped. An error is displayed if the given generation does'nt exists. Default is $generation" | /usr/bin/fmt -w $col
	echo "        -L, --skip-live"
	echo "            This option is useful when resuming an evolution with -g. It tells \
the script to resume evolution after the life of the given generation, in other words, \
directly with survive, reproduce and mutate. After that, the cycles continue normally." | /usr/bin/fmt -w $col
	echo "        --minmutate M"
	echo "            Defines the minimum value of the mutation interval" | /usr/bin/fmt -w $col
	echo "        --maxmutate M"
	echo "            Defines the maximum value of the mutation interval" | /usr/bin/fmt -w $col
	echo "        -s, --survive S"
	echo "            Defines how many sets will survive for each generation. It can be \
a percentage, in that case type a number immediatly followed by '%'. Default is $survive" | /usr/bin/fmt -w $col
	echo "        -E, --skip-evolve"
	echo "            Disables evolution"
	echo
	write_bold "    Command Execution"
	echo
	echo "        All of the following options are required for evolving"
	echo "        -e, --exec-cmd CMD"
	echo "            The command line to execute"
	echo "        -d, --exec-dir DIR"
	echo "            Set the directory in which to launch the given command" | /usr/bin/fmt -w $col
	echo "        -r, --exec-result FILE"
	echo "            The file in which the script will search for result (with the \
retrieve function, see option --result-f). This is a constraint. If your command \
outputs result on stdout, you'll have to redirect that result in some file WITHIN \
the command, like this: --exec-cmd 'your command > some_file'." | /usr/bin/fmt -w $col
	echo "        --ignore-pre-exec-error"
	echo "            Ignore pre-execution error status. See -i option"
	echo "        --ignore-exec-error"
	echo "            Ignore execution error status. See -i option"
	echo "        --ignore-post-exec-error"
	echo "            Ignore post-execution error status. See -i option"
	echo "        -i OCTAL_IGN"
	echo "            Ignore error status. pre-exec is 1, exec is 2, post-exec is 4. \
3 is pre and exec, 5 is pre and post, 6 is exec and post, 7 is ignore all" | /usr/bin/fmt -w $col
	echo
	write_bold "    Pre, Post & Result Functions"
	echo
	echo "        All global variables (check Global Variables section in the script) \
can be used in these functions, in addition to exec_cmd, exec_dir and exec_result. \
Only --result-f is required" | /usr/bin/fmt -w $col
	echo "        --pre-f FILE"
	echo "            FILE contains the pre-execution function. This function must be \
named 'pre_exec'. It uses the path to one set as first and only argument" | /usr/bin/fmt -w $col
	echo "        --post-f FILE"
	echo "            FILE contains the post-execution function. This function must be \
named 'post_exec'. It uses the path to one set as first and only argument" | /usr/bin/fmt -w $col
	echo "        --result-f FILE"
	echo "            FILE contains the retrieving result function. This function must be \
named 'retrieve_result'. This function MUST output the result on stdout" | /usr/bin/fmt -w $col
	echo
	write_bold "    Directories"
	echo
	echo "        --set-dir DIR"
	echo "            Sets the directory where all sets are put. \
Default is $set_dir_root" | /usr/bin/fmt -w $col
	echo "        --result-dir DIR"
	echo "            Sets the directory where all results are put. \
Default is $result_dir_root" | /usr/bin/fmt -w $col
	echo "        --survive-dir DIR"
	echo "            Sets the directory where temporary survivors are put. \
Default is $survive_dir" | /usr/bin/fmt -w $col
	echo "        --children-dir DIR"
	echo "            Sets the directory where temporary children are put. \
Default is $children_dir" | /usr/bin/fmt -w $col
	echo
	write_bold "Exit status"
	echo "    0: Success"
	echo "    1: Command line syntax error / Unfoundable directory or file"
	echo "    2: Execution error, user stopped"
	echo "    3: Error while mkdir, invalid directory name"
	exit 0
}

# generate random float
random_float() {
	# usage: random_float min max scale
	echo "scale=$3; ($RANDOM*$2*2/32767)+$1" | /usr/bin/bc
}

# generate random int
random_int() {
	# usage: random_int min max
	echo "($RANDOM*$2*2/32767)+$1" | /usr/bin/bc
}

# float average of two numbers
average_float() {
	# usage: average_float A B scale
	echo "scale=$3; ($1+$2)/2" | /usr/bin/bc
}

# int average of two numbers
average_int() {
	# usage: average_int A B
	echo "($1+$2)/2" | /usr/bin/bc
}

# alterate one parameter with a random float value
alterate_float() {
	# usage: alterate_parameter p min max scale
	local np=$(echo "scale=$4; $1+($(random_float $2 $3 $4))" | /usr/bin/bc)
	# prefix m with 0 if needed
	[ "${np:0:1}" = "."  ] && np="0$np"
	[ "${np:0:2}" = "-." ] && np="-0${np:1}"
	echo "$np"
}

# alterate one parameter with a random int value
alterate_int() {
	# usage: alterate_parameter p min max
	local np=$(echo "$1+($(random_int $2 $3))" | /usr/bin/bc)
	echo "$np"
}

# write some information on stderr
message() {
	echo "$*" >&2
}

# get param i from a set
get_param() {
	# usage: get_param i set
	# i start at 0, so we add 1 for 'head' option
	local line=$1
	local p=$(/usr/bin/head -n$((line+1)) "$2")
	p=$(echo "$p" | /usr/bin/tail -n1)
	p=${p#*=}
	echo "$p"
}

# search for last born set
get_last_set() {
	local gen s
	local ls=0
	last_set=0
	for ((gen=0; gen<=$generation; gen++)); do
		s=$(/bin/ls -1v "$set_dir_root/generation$gen" | /usr/bin/tail -n1)
		s=${s##*t}
		[ $s -gt $ls ] && ls=$s
	done
	echo $ls
}			

# change set_dir and result_dir to next generation (side effect)
next_generation() {
	# usage: next_generation
	let generation++
	set_dir="${set_dir%/*}/generation$generation"
	mkdir "$set_dir" 2>/dev/null
	result_dir="${result_dir%/*}/generation$generation"
	mkdir "$result_dir" 2>/dev/null
}

# runs exec_cmd given ONE set of parameters
run_set() {
	# usage: run_set set_path
	local ret_pre=1
	local ret_post=1

	if [ $opt_pref -eq 1 ]; then
		pre_exec "$1"
		ret_pre=$?
		[[ $ign_pre -eq 0 && $ret_pre -ne 0 ]] && return 1
	fi
	
	pushd "$exec_dir" 2>/dev/null >&2
	eval $exec_cmd
	ret_exec=$?
	popd 2>/dev/null >&2
	[[ $ign_exec -eq 0 && $ret_exec -ne 0 ]] && return 2 

	if [ $opt_postf -eq 1 ]; then
		post_exec "$1"
		ret_post=$?
		[[ $ign_post -eq 0 && $ret_post -ne 0 ]] && return 3
	fi
	
	return 0
}

# get pre, post, retrieve function
function_source() {
	# usage: function_source <function_name> <file>
	if [ -f "$2" ]; then
		if [ -n "$(/bin/grep "$1()" "$2")" ]; then
			. "$2"
		else
			echo "function must be named '"$1"'" >&2
			exit 1
		fi
	else
		echo "$2: no such $1 function file" >&2
		exit 1
	fi
}

# get parameters
param_source() {
	if [ -f "$1" ]; then
		. "$1"
	else
		echo "** $1: no such parameters file" >&2
		exit 1
	fi
}

# control
is_integer() {
	printf "%d" "$1" 2>/dev/null >&2
}
is_positive() {
	is_integer "$1" && [ "${1:0:1}" != "-" ]
}
is_positive_notnull() {
	is_positive "$1" && [ $1 -gt 0 ]
}
missing() {
	echo "$1 argument is missing" >&2
	opt_error=1
}

# individual mutate
individual_mutation() {
	# usage: child_path
	# get parameters, alterate them
	local i
	for ((i=0; i<${#param[@]}; i++)); do
		p[$i]=$(get_param $i "$1")
		p[$i]=$($alterate ${p[$i]} $min_mutate $max_mutate $scale)
	done
	# outputs alterated parameters into child (overwrite previous parameters)
	for ((i=0; i<${#param[@]}; i++)); do
		echo "${param[$i]}=${p[$i]}"
	done
}

# creates N sets from one base set, with mutations
set_based_mutation_init() {
	# usage: base_set
	local i
	for ((i=0; i<$N; i++)); do
		individual_mutation "$1" > "$set_dir/set$i"
	done
}

# creates N sets of random parameters
create_life() {
	# usage: create_life
	local i value
	for ((i=0; i<$N; i++)); do
		echo -n > "$set_dir/set$i"
		for ((j=0; j<${#param[@]}; j++)); do
			value=$($random $min_value $max_value $scale)
			# prefix value with 0 if needed
			[ "${value:0:1}" = "."  ] && value="0$value"
			[ "${value:0:2}" = "-." ] && value="-0${value:1}"
			echo "${param[$j]}=$value" >> "$set_dir/set$i"
		done
	done
}

# runs exec_cmd with ALL N sets of current generation
live() {
	# usage: live
	local i continue res ret_exec
	# run the current generation sets sequentially
	for i in $(/bin/ls -1v "$set_dir"); do
		if [ -f "$result_dir/result${i##*t}" ]; then
			echo -n "$exec_cmd with $i... "
			echo "scored $(/bin/cat "$result_dir/result${i##*t}")"
			continue
		fi
		
		echo -n "$exec_cmd with $i... "
		if ! run_set "$set_dir/$i"; then  
			case $? in
				1) echo "failed :/ (pre_exec)" ;;
				2) echo "failed :/ ($exec_cmd returned code $ret_exec)" ;;
				3) echo "failed :/ (post_exec)" ;;
			esac
			# pre/post/exec failed, wait interaction
			echo -n "Continue ? "
			read continue
			case $continue in
				"") ;;
				n*|N*) exit 2 ;;
				*) ;;
			esac
		else
			res="$(retrieve_result)"
			res=${res%% *}
			echo "scored $res"
			echo "$res" > "$result_dir/result${i##*t}"
		fi
	done
}

# saves the best sets into the next generation
survive() {
	# usage: survive
	# this function uses ONE VALUE RESULTS
	# you'll have to edit it if you have MULTIPLE VALUE RESULTS
	local continue sorted opt_reverse
	local how_many_survive survivors survivor
	
	case $fittest_is in
		'l') opt_reverse=0 ;;
		'h') opt_reverse=1 ;;
	esac
	
	for result in "$result_dir"/result*; do
		sorted="${sorted}$(/bin/cat "$result" 2>/dev/null):${result##*t}\n"
	done
	if [ $opt_reverse -eq 1 ]; then
		sorted="$(echo -e "$sorted" | /usr/bin/sort -r -g)"
		sorted="$(echo "$sorted")" # | /usr/bin/head -n-1)"
	else
		sorted="$(echo -e "$sorted" | /usr/bin/sort -g)"
		sorted="$(echo "$sorted" | /usr/bin/tail -n+2)"
	fi
	sorted="$(echo "$sorted" | /usr/bin/cut -d':' -f2)"
	
	case $survive in
		*%) how_many_survive=$(($(echo "$sorted" | /usr/bin/wc -l)*${survive%\%}/100)) ;;
		*) how_many_survive=$survive ;;
	esac
	
	[ $how_many_survive -eq 0 ] && return 1
	
	survivors=$(echo "$sorted" | /usr/bin/head -n$how_many_survive)
	
	# temporary location for survivors
	for survivor in $survivors; do
		/bin/cp "$set_dir/set$survivor" "$survive_dir"
	done
	
	# we  create the next generation
	# indeed, survivors live during at least the next generation
	next_generation
	
	# we move all survivors into the next generation
	for survivor in "$survive_dir"/*; do
		/bin/mv "$survivor" "$set_dir"
	done
}

# outputs couples of survivor sets (each possible combination)
# yes, they love to reproduce themselves, and don't ask me about genders...
couple() {
	# usage: couple
	local sets[0]=
	local count=0
	local i
	for i in "$set_dir"/*; do
		sets[$count]="$i"
		let count++
	done
	
	local j
	for ((i=0; i<${#sets[@]}-1; i++)); do
		for ((j=i; j<${#sets[@]}-1; j++)); do
			echo "${sets[$i]##*t}:${sets[$((j+1))]##*t}"
		done
	done
}

# uses couple function to give birth to children (one per couple)
give_life() {
	# usage: give_life
	local cpl
	local setA setB newSet
	local i avg
	local paramA paramB
	
	for cpl in $(couple); do
		let last_set++
		setA="$set_dir/set${cpl%:*}"
		setB="$set_dir/set${cpl#*:}"
		newSet="$children_dir/set$last_set"
		echo -n > "$newSet"
		for ((i=0; i<${#param[@]}; i++)); do
			paramA=$(get_param $i "$setA")
			paramB=$(get_param $i "$setB")
			avg=$($average $paramA $paramB $scale)
			echo "${param[$i]}=$avg" >> "$newSet"
		done
	done
}

# alterate each parameter of each newly created set randomly
# read child number from stdin
mutate() {
	# usage: mutate
	local child
	local i p
	# mutate children at the nursery
	for child in "$children_dir"/*; do
		individual_mutation "$child" > "${child}_tmp"
		/bin/mv "${child}_tmp" "$child"
	done
	# move children from nursery to current generation
	for child in "$children_dir"/*; do
		/bin/mv "$child" "$set_dir/"
	done
}

# all steps but living, useful for resuming evolution at a specific generation
after_life() {
	if ! survive; then
		echo "All sets are dead... the game is over !"
		exit 0
	fi
	give_life
	mutate
}

# one cycle evolution (live, survive, couple, give_life, and mutate)
evolution() {
	# usage: evolution
	echo "Generation $generation:"
	live
	after_life
}

# show all result, non-sorted
show_all() {
	local res s score
	for res in "$result_dir_root"/*; do
		for s in "$res"/*; do
			score="$(/bin/cat "$s")"
			[ -z "$score" ] && continue
			echo "$score:${s##*t}:${s##*n}"
		done
	done
}

# show sorted result with unique indexes
show_sorted_unique() {
	local r tab='-'
	local score setn gen
	local rev
	[ $opt_show_reverse -eq 1 ] && rev='-r'
	echo "|   Index    |   Score    | Generation  |"
	echo "|---------------------------------------|"
	for r in $(show_all | /usr/bin/sort -g $rev); do
		score=${r%%:*}
		r=${r#*:}
		setn=${r%:*}
		gen=${r#*:}
		gen=${gen%/*}
		if ! echo "$tab" | /bin/grep -q "\-$setn\-"; then
			printf "| Set %-3d    |    %-4d    |   g%-3s      |\n" $setn $score $gen
			tab="${tab}$setn-"
		fi
	done
}

# show sorted result
show_sorted() {
	local r score setn gen
	local rev
	[ $opt_show_reverse -eq 1 ] && rev='-r'
	echo "|   Index    |   Score    | Generation  |"
	echo "|---------------------------------------|"
	for r in $(show_all | /usr/bin/sort -g $rev); do
		score=${r%%:*}
		r=${r#*:}
		setn=${r%:*}
		gen=${r#*:}
		gen=${gen%/*}
		printf "| Set %-3d    |    %-4d    |   g%-3s      |\n" $setn $score $gen
	done
}

# show sum results (by set)
show_sum_set() {
	local i=0
	local setn sumset
	local sets="$(/usr/bin/find "$result_dir_root" -name result$i)"
	while [ -n "$sets" ]; do
		sumset=0
		for setn in $sets; do
			sumset=$(($sumset+$(/bin/cat "$setn")))
		done
		echo "$sumset:${setn##*t}"
		let i++
		sets="$(/usr/bin/find "$result_dir_root" -name result$i)"
	done
}

# show sorted sum results (by set)
show_sorted_sum_set() {
	local res
	local rev
	[ $opt_show_reverse -eq 1 ] && rev='-r'
	echo "|   Index    |    Sum     |"
	echo "|-------------------------|"
	for res in $(show_sum_set | /usr/bin/sort -g $rev); do
		printf "| Set %-3d    |   %-6d   |\n" ${res#*:} ${res%:*}
	done
}

# show sum results (by gen)
show_sum_gen() {
	local gen sumgen res
	for gen in "$result_dir_root"/*; do
		sumgen=0
		for res in "$gen"/*; do
			sumgen=$(($sumgen+$(/bin/cat "$res")))
		done
		echo "$sumgen:${gen##*n}"
	done
}

# show sorted sum results (by gen)
show_sorted_sum_gen() {
	local res
	local rev
	[ $opt_show_reverse -eq 1 ] && rev='-r'
	echo "|   Index    |    Sum     |"
	echo "|-------------------------|"
	for res in $(show_sum_gen | /usr/bin/sort -g $rev); do
		printf "| Gen %-3d    |   %-6d   |\n" ${res#*:} ${res%:*}
	done
}

# show average of each set
show_average_set() {
	local i=0
	local setn avgset count sumset
	local sets="$(/usr/bin/find "$result_dir_root" -name result$i)"
	while [ -n "$sets" ]; do
		count=0
		avgset=0
		sumset=0
		for setn in $sets; do
			sumset=$(($sumset+$(/bin/cat "$setn")))
			let count++
		done
		avgset=$((sumset/count))
		echo "${setn##*t}:$avgset"
		let i++
		sets="$(/usr/bin/find "$result_dir_root" -name result$i)"
	done
}

# creates a png file with a graphic showing evolution by set
gnuplot_evolution_set() {
	# output data in a temp file
	local data="$survive_dir/data"
	local script="$survive_dir/gnuscript"
	local res maxscore=0
	
	for res in $(show_average_set); do
		echo "${res//:/ }"
		[ ${res#*:} -gt $maxscore ] && maxscore=${res#*:}
	done > "$data"
	
	/bin/cat "$data" | /usr/bin/sort -g > "${data}_tmp"
	/bin/mv "${data}_tmp" "$data"
	
	# sets some values
	local maxsize=$(/bin/cat "$data" | /usr/bin/tail -n1)
	maxsize=${maxsize%% *}
	local xtics=$maxsize
	
	if [ $xtics -gt 160 ]; then xtics=16
	elif [ $xtics -gt 80 ]; then xtics=8
	elif [ $xtics -gt 40 ]; then xtics=4
	elif [ $xtics -gt 20 ]; then xtics=2
	else xtics=1; fi
	
	# write the script
	{
		echo set terminal png size 800, 400
		echo set terminal png enhanced
		echo set terminal png font arial 10
		echo set output \"./graphset.png\"
		echo set key on inside top left box
		echo set xrange [0:$maxsize]
		echo set yrange [0:$maxscore]
		echo set xtics $xtics
		echo set grid
		echo plot \"$data\" using 1:2 title \'lines\' with linespoint
	} > "$script"
	
	# execute the script in gnuplot
	/usr/bin/gnuplot < "$script"
	
	# clean temporary data
	/bin/rm "$data"
	/bin/rm "$script"
}

# creates a png file with a graphic showing evolution by gen
gnuplot_evolution_gen() {
	# output data in a temp file
	local data="$survive_dir/data"
	local script="$survive_dir/gnuscript"
	local maxscore=0
	
	local gen sumgen res
	for gen in "$result_dir_root"/*; do
		sumgen=0
		for res in "$gen"/*; do
			sumgen=$(($sumgen+$(/bin/cat "$res")))
		done
		[ $sumgen -gt $maxscore ] && maxscore=$sumgen
		echo "${gen##*n} $sumgen"
	done > "$data"
	
	/bin/cat "$data" | /usr/bin/sort -g > "${data}_tmp"
	/bin/mv "${data}_tmp" "$data"
	
	# sets some values
	local maxsize=$(/bin/cat "$data" | /usr/bin/tail -n1)
	maxsize=${maxsize%% *}
	local xtics=$maxsize
	
	if [ $xtics -gt 160 ]; then xtics=16
	elif [ $xtics -gt 80 ]; then xtics=8
	elif [ $xtics -gt 40 ]; then xtics=4
	elif [ $xtics -gt 20 ]; then xtics=2
	else xtics=1; fi
	
	# write the script
	{
		echo set terminal png size 800, 400
		echo set terminal png enhanced
		echo set terminal png font arial 10
		echo set output \"./graphgen.png\"
		echo set key on inside top left box
		echo set xrange [0:$maxsize]
		echo set yrange [0:$maxscore]
		echo set xtics $xtics
		echo set grid
		echo plot \"$data\" using 1:2 title \'lines\' with linespoint
	} > "$script"
	
	# execute the script in gnuplot
	/usr/bin/gnuplot < "$script"
	
	# clean temporary data
	/bin/rm "$data"
	/bin/rm "$script"
}


#**************************************
#		Main
#**************************************

# arguments analysis
[ $# -eq 0 ] && usage

[[ "$1" = "-h" || "$1" = "--help" ]] && help

if [ "$1" = "--show" ]; then
	shift
	while [ $# -ne 0 ]; do
		case $1 in
			"-d"|"--directory")
				if [ -d "$2" ]; then
					result_dir_root="$2"
					shift
				else
					echo "$2: not a valid directory" >&2
					exit 1
				fi
			;;
			"-u"|"--unique") opt_show_unique=1 ;;
			"-r"|"--reverse") opt_show_reverse=1 ;;
			"-s"|"--sum")
				case $2 in
					"set"|"gen") opt_show_sum=$2 ;;
					"generation") opt_show_sum="gen" ;;
					*)
						echo "$2: unsupported sum, use with 'set' or 'gen'" >&2
						exit 1
					;;
				esac
				shift
			;;
			"-g"|"--graph")
				case $2 in
					"set"|"gen") opt_plot=$2 ;;
					"generation") opt_plot="gen" ;;
					*)
						echo "$2: unsupported graph, use with 'set' or 'gen'" >&2
						exit 1
					;;
				esac
				shift
			;;
			*)
				echo "$1: unknow argument" >&2
				usage
			;;
		esac
		shift
	done
	
	if [ -n "$opt_plot" ]; then
		gnuplot_evolution_$opt_plot
	elif [ -n "$opt_show_sum" ]; then
		show_sorted_sum_$opt_show_sum | /bin/more
	elif [ $opt_show_unique -eq 1 ]; then
		show_sorted_unique | /bin/more
	else
		show_sorted | /bin/more
	fi
	
	exit 0
fi

while [ $# -ne 0 ]; do
	case $1 in
		"-c"|"--cycle")
			if is_positive "$2"; then
				cycle=$2
				shift
			else
				echo "$2: cycle value has to be a positive integer (0 allowed)" >&2
				exit 1
			fi
		;;
		"--children-dir")
			children_dir="$2"
			shift
		;;
		"-d"|"--exec-dir")
			if [ -d "$2" ]; then
				exec_dir="$2"
				shift
			else
				echo "$2: not a valid directory" >&2
				exit 1
			fi
		;;
		"-e"|"--exec-cmd")
			exec_cmd="$2"
			shift
		;;
		"-E"|"--skip-evolve") opt_skipevolve=1 ;;
		"-f"|"--fittest")
			case $2 in
				'h'|"highest") fittest_is='h' ;;
				'l'|"lowest") fittest_is='l' ;;
				*)
					echo "$2: unsupported, please use with highest (or h) or lowest (or l)" >&2
					exit 1
				;;
			esac
			shift
		;;
		"-g"|"--generation")
			if is_positive "$2"; then
				generation=$2
				shift
			else
				echo "$2: generation has to be an integer (0 allowed)" >&2
			fi
		;;
		"--init-set")
			if is_positive_notnull "$2"; then
				N=$2
				shift
			else
				echo "$2: value for init sets has to be a positive integer" >&2
				exit 1
			fi
		;;
		"-I"|"--skip-init") opt_skipinit=1 ;;
		"-i"[1243567])
			case ${1:2} in
				7|"")
					ign_pre=1
					ign_exec=1
					ign_post=1
				;;
				6)
					ign_exec=1
					ign_post=1
				;;
				5)
					ign_pre=1
					ign_post=1
				;;
				3)
					ign_pre=1
					ign_exec=1
				;;
				4) ign_post=1 ;;
				2) ign_exec=1 ;;
				1) ign_pre=1 ;;
			esac
		;;
		"--ignore-pre-exec-error") ign_pre=1 ;;
		"--ignore-exec-error") ign_exec=1 ;;
		"--ignore-post-exec-error") ign_post=1 ;;
		"-L"|"--skip-live") opt_skiplive=1 ;;
		"--minvalue")
			if is_integer "$2"; then
				min_value=$2
				shift
			else
				echo "$2: min_value has to be an integer" >&2
				exit 1
			fi
		;;
		"--maxvalue")
			if is_integer "$2"; then
				max_value=$2
				shift
			else
				echo "$2: max_value has to be an integer" >&2
				exit 1
			fi
		;;
		"--minmutate")
			if is_integer "$2"; then
				min_mutate=$2
				shift
			else
				echo "$2: min_mutate has to be an integer" >&2
				exit 1
			fi
		;;
		"--maxmutate")
			if is_integer "$2"; then
				max_mutate=$2
				shift
			else
				echo "$2: max_mutate has to be an integer" >&2
				exit 1
			fi
		;;
		"-m"|"--mutate-from")
			if [ -f "$2" ]; then
				opt_mutate=1
				base_set="$2"
				shift
			else
				echo "$2: no such base set" >&2
				exit 1
			fi
		;;
		"-p"|"--parameters")
			opt_param=1
			param_source "$2"
			shift
		;;
		"--post-f")
			opt_postf=1
			function_source 'post_exec' "$2"
				shift
		;;
		"--pre-f")
			opt_pref=1
			function_source 'pre_exec' "$2"
				shift
		;;
		"-r"|"--exec-result")
			if [ -n "$2" ]; then
				exec_result="$2"
				[ ! -f "$2" ] && echo "warning: $2: unfoundable result file" >&2
				shift
			fi
		;;
		"--result-f")
			opt_resf=1
			function_source 'retrieve_result' "$2"
				shift
		;;
		"--result_dir")
			result_dir="$2"
			shift
		;;
		"--set-dir")
			set_dir="$2"
			shift
		;;
		"-s"|"--survive")
			if is_positive_notnull "${2%\%}"; then
				survive="$2"
				shift
			else
				echo "$2: not valid, please use with number or percentage%" >&2
				exit 1
			fi
		;;
		"--survive-dir")
			survive_dir="$2"
			shift
		;;
		"-S"|"--scale")
			if is_positive_notnull "$2"; then
				scale=$2
				shift
			else
				echo "$2: scale has to be a positive integer" >&2
				exit 1
			fi
		;;
		"-t"|"--type")
			case $2 in
				"int"|"integer") type=int ;;
				"float") type=float;;
				*)
					echo "$2: unsupported type, please use with int or float" >&2
					exit 1
				;;
			esac
			shift
		;;
		*)
			echo "$1: unknow argument" >&2
			usage
		;;
	esac
	shift
done

# check type/scale and init behavior
if [ -z "$type" ]; then
	missing "--type"
else
	case $type in
		"int")
			random=random_int
			average=average_int
			alterate=alterate_int
		;;
		"float")
			[ -z "$scale" ] && missing "--scale"
			random=random_float
			average=average_float
			alterate=alterate_float
		;;
	esac
fi

# ever required args
[ $opt_param -eq 0  ] && missing "--parameters"

# required args for evolving
if [ $opt_skipevolve -eq 0 ]; then
	[ -z "$exec_cmd"    ] && missing "--exec-cmd"
	[ -z "$exec_dir"    ] && missing "--exec-dir"
	[ -z "$exec_result" ] && missing "--exec-result"
	[ -z "$min_mutate"  ] && missing "--minmutate"
	[ -z "$max_mutate"  ] && missing "--maxmutate"
	[ $opt_resf -eq 0   ] && missing "--result-f"
fi


# required args for creating life
if [[ $opt_skipinit -eq 0 || $opt_mutate -eq 1 ]]; then
	[ -z "$N" ] && missing "--init-set"
fi

# no need of these if init with mutation
if [[ $opt_skipinit -eq 0 && $opt_mutate -eq 0 ]]; then
	[ -z "$min_value" ] && missing "--minvalue"
	[ -z "$max_value" ] && missing "--maxvalue"
fi

# abort if error
[ $opt_error -eq 1 ] && exit 1

# all good, init directories (exit 3 if error while mkdir)
[ ! -d "$set_dir_root"    ] && { mkdir -p "$set_dir_root"      || exit 3; }
[ ! -d "$result_dir_root" ] && { mkdir -p "$result_dir_root"   || exit 3; }
[ ! -d "$survive_dir"     ] && { mkdir -p "$survive_dir"  || exit 3; }
[ ! -d "$children_dir"    ] && { mkdir -p "$children_dir" || exit 3; }

# init set and results directory for given generation
set_dir="$set_dir_root/generation$generation"
result_dir="$result_dir_root/generation$generation"

# generation>0 skips creating first sets, either with mutate or not
if [ $generation -eq 0 ]; then
	[ ! -d "$set_dir" ] && mkdir -p "$set_dir"
	# generates very first set with mutations from a base set (executed even with --skip-init)
	if [ $opt_mutate -eq 1 ]; then
		last_set=$((N-1))
		set_based_mutation_init "$base_set"
	else
		# generates very first sets (skipped if --skip-init, generation>0, or opt_mutate=1)
		if [ $opt_skipinit -eq 0 ]; then
			last_set=$((N-1))
			create_life
		else
			last_set=$(/bin/ls -1v "$set_dir" | /usr/bin/tail -n1)
			last_set=${last_set##*t}
		fi
	fi
else
	[ ! -d "$set_dir" ] && { echo "$set_dir: no such generation" >&2; exit 1; }
	last_set=$(get_last_set)
fi

# let's get evolve (skipped if --skip-evolve)
if [ $opt_skipevolve -eq 0 ]; then
	[ ! -d "$result_dir" ] && mkdir -p "$result_dir"
	# with --skip-live option, resume without 'live' step
	if [ $opt_skiplive -eq 1 ]; then
		echo "Resume after life at generation $generation..."
		after_life
	fi
	case $cycle in
		0)
			while true; do
				evolution
			done
		;;
		*)
			for ((e=0; e<$cycle; e++)); do
				evolution
			done
		;;
	esac
fi

		
