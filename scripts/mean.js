const fs = require('fs')
const path = require('path')
const glob = require('glob')
const csv = require('fast-csv')
const rimraf = require('rimraf')

const args = process.argv;
console.log('Arguments: ', args)
if(args.length < 3) {
    console.log("Please provide at least a folder where your results are.")
    process.exit(1)
} else {
    console.log('Reading folder: ', args[2])
    const directory = args[2]
    const pattern  =  directory + "p*conf.txt"
    // options is optional
    glob(pattern, {}, function (er, files) {
        // files is an array of filenames.
        // If the `nonull` option is set, and nothing
        // was found, then files is ["**/*.js"]
        // er is an error object or null.
        if(files.length > 0){
            rimraf.sync(directory + "*-mean.csv")
            processAverage(files, directory)
        }
    })
}


function processAverage(files, directory) {
    const all = []
    files.forEach(f => all.push(processFile(f, directory)))
    Promise.all(all).then(() => {
        console.log(`Parsing all files from ${directory} finished`)
    })
}

function processFile(file, directory) {
    return new Promise((resolve, reject) => {
        try{
            console.log('Parsing ... %s', file)
            const files = file.split('-')
            let average = undefined
            let max = 0;
            csv.fromPath(file).on("data", function(data){
                max++
                if(!average) {
                    average = data.map(d => eval(d))
                } else {
                    for (let i = 0; i < data.length; i++) {
                        average[i] += eval(data[i])
                    }
                }
            }).on("end", function(){
                for (let i = 0; i < average.length; i++) {
                    average[i] = average[i] / max
                }
                const Q = files[2]
                const SON = files[3]
                const TRAFFIC = files[5]
                average.push(Q, SON, TRAFFIC)
                const output = path.resolve(`${directory}/${Q}${SON}${TRAFFIC}-mean.csv`)
                const output2 = path.resolve(`${directory}/${SON}${TRAFFIC}-mean.csv`)
                const global = path.resolve(`${directory}/global-mean.csv`)
                // console.log('Appending into:', output, output2, global)
                let stream = fs.createWriteStream(output, {flags:'a'});
                stream.write(average.join(',') + '\n')
                stream.end()

                stream = fs.createWriteStream(output2, {flags:'a'});
                stream.write(average.join(',') + '\n')
                stream.end()

                stream = fs.createWriteStream(global, {flags:'a'});
                stream.write(average.join(',') + '\n')
                stream.end()
                resolve()
            }).on('error', function(error) {
                reject(error)
            })
        } catch(error) {
            reject(error)
        }
    })
}